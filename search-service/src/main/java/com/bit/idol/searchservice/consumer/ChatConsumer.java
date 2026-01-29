package com.bit.idol.searchservice.consumer;

import com.bit.idol.searchservice.document.ChatDocument;
import com.bit.idol.searchservice.repository.ChatSearchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatConsumer {

    private final ChatSearchRepository chatSearchRepository;
    private final ObjectMapper objectMapper;

    // Batch Listener 적용 (application.yml 설정 필요: listener.type: batch)
    @KafkaListener(topics = "chat-topic", groupId = "search-service-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(List<String> messages) {
        
        List<ChatDocument> documents = new ArrayList<>();

        for (String message : messages) {
            try {
                Map<String, Object> map = objectMapper.readValue(message, Map.class);

                String id = (String) map.get("id");
                Long idolId = Long.valueOf(String.valueOf(map.get("idolId")));
                Integer senderId = (Integer) map.get("senderId");
                String senderNickname = (String) map.get("senderNickname");
                String senderRole = (String) map.get("senderRole");
                String content = (String) map.get("content");
                String type = (String) map.get("type");
                
                // 날짜 파싱 로직 보완 필요 (여기서는 현재 시간)
                LocalDateTime createdAt = LocalDateTime.now();

                ChatDocument document = ChatDocument.builder()
                        .id(id)
                        .idolId(idolId)
                        .senderId(senderId)
                        .senderNickname(senderNickname)
                        .content(content) // Nori 분석기 적용됨
                        .createdAt(createdAt)
                        .build();
                
                documents.add(document);

            } catch (Exception e) {
                log.error("Kafka 메시지 파싱 실패: {}", message, e);
            }
        }

        if (!documents.isEmpty()) {
            chatSearchRepository.saveAll(documents); // Bulk Indexing
            log.info("Elasticsearch Bulk Indexing 완료: {}건", documents.size());
        }
    }
}
