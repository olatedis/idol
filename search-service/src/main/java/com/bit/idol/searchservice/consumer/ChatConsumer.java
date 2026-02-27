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

    @KafkaListener(topics = "${spring.kafka.topic.chat-message}", groupId = "search-service-group", containerFactory = "kafkaListenerContainerFactory")
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

                // 텍스트(TEXT) 타입이 아닌 경우 (IMAGE, VIDEO, SYSTEM 등)는 검색 대상에서 제외
                if (!"TEXT".equals(type)) {
                    continue;
                }

                // 날짜 파싱 로직 (Kafka 페이로드에 따라 String 혹은 List 형태로 들어옴)
                LocalDateTime createdAt = LocalDateTime.now();
                Object createdAtObj = map.get("createdAt");
                if (createdAtObj != null) {
                    try {
                        if (createdAtObj instanceof String) {
                            String dateStr = (String) createdAtObj;
                            // 소수점 초 단위가 있을 수 있으므로 자르거나 그대로 파싱
                            if (dateStr.contains(".")) {
                                dateStr = dateStr.substring(0, dateStr.indexOf("."));
                            }
                            createdAt = LocalDateTime.parse(dateStr);
                        } else if (createdAtObj instanceof List) {
                            List<Integer> list = (List<Integer>) createdAtObj;
                            int year = list.size() > 0 ? list.get(0) : 0;
                            int month = list.size() > 1 ? list.get(1) : 1;
                            int day = list.size() > 2 ? list.get(2) : 1;
                            int hour = list.size() > 3 ? list.get(3) : 0;
                            int minute = list.size() > 4 ? list.get(4) : 0;
                            int second = list.size() > 5 ? list.get(5) : 0;
                            createdAt = LocalDateTime.of(year, month, day, hour, minute, second);
                        }
                    } catch (Exception e) {
                        log.warn("날짜 파싱 실패, 현재 시간으로 대체: {}", createdAtObj);
                    }
                }

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
