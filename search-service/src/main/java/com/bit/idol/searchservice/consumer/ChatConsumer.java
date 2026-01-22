package com.bit.idol.searchservice.consumer;

import com.bit.idol.searchservice.document.ChatDocument;
import com.bit.idol.searchservice.repository.ChatSearchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatConsumer {

    private final ChatSearchRepository chatSearchRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "chat-topic", groupId = "search-service-group")
    public void consume(String message) {
        try {
            // Kafka 메시지(JSON) 파싱
            // ChatMessageDto 구조와 비슷하다고 가정
            Map<String, Object> map = objectMapper.readValue(message, Map.class);

            String id = (String) map.get("id");
            // idolId가 Integer로 올 수도 있고 Long으로 올 수도 있어서 안전하게 변환
            Long idolId = Long.valueOf(String.valueOf(map.get("idolId")));
            Integer senderId = (Integer) map.get("senderId");
            String senderNickname = (String) map.get("senderNickname");
            String senderRole = (String) map.get("senderRole");
            String content = (String) map.get("content");
            String type = (String) map.get("type");
            
            // 날짜 처리 (문자열로 온다면 파싱 필요, 여기서는 현재 시간으로 대체하거나 파싱 로직 추가)
            // 실제로는 DTO에 createdAt이 있다면 그걸 써야 함
            LocalDateTime createdAt = LocalDateTime.now();

            // Elasticsearch 문서 생성
            ChatDocument document = ChatDocument.builder()
                    .id(id)
                    .idolId(idolId)
                    .senderId(senderId)
                    .senderNickname(senderNickname)
                    .senderRole(senderRole)
                    .content(content)
                    .type(type)
                    .createdAt(createdAt)
                    .build();

            // 저장 (Indexing)
            chatSearchRepository.save(document);
            log.debug("Elasticsearch 저장 완료: id={}, content={}", id, content);

        } catch (Exception e) {
            log.error("Kafka 메시지 처리 실패: {}", e.getMessage());
        }
    }
}
