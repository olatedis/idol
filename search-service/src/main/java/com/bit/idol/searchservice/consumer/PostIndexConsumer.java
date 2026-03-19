package com.bit.idol.searchservice.consumer;

import com.bit.idol.searchservice.document.PostDocument;
import com.bit.idol.searchservice.kafka.PostIndexEvent;
import com.bit.idol.searchservice.repository.PostSearchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostIndexConsumer {

    private final PostSearchRepository postSearchRepository;
    private final ObjectMapper objectMapper;

    // board-post-index-topic 으로 UPSERT/DELETE 이벤트를 받아 ES에 반영
    // Batch Listener 적용 전제(application.yml에서 listener.type=batch)
    @KafkaListener(
            topics = "board-post-index-topic",
            groupId = "search-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(List<String> messages) {
        List<PostDocument> upserts = new ArrayList<>();
        List<String> deletes = new ArrayList<>();

        for (String message : messages) {
            try {
                PostIndexEvent event = objectMapper.readValue(message, PostIndexEvent.class);

                if ("DELETE".equalsIgnoreCase(event.getAction())) {
                    deletes.add(String.valueOf(event.getPostId()));
                    continue;
                }

                // UPSERT
                PostDocument doc = PostDocument.builder()
                        .id(String.valueOf(event.getPostId()))
                        .postId(event.getPostId())
                        .boardType(event.getBoardType())
                        .idolId(event.getIdolId())
                        .groupId(event.getGroupId())
                        .title(event.getTitle())
                        .content(event.getContent())
                        .createdAt(parseToDate(event.getCreatedAt()))
                        .updatedAt(parseToDate(event.getUpdatedAt()))
                        .build();

                upserts.add(doc);

            } catch (Exception e) {
                log.error("Kafka 메시지 파싱 실패: {}", message, e);
            }
        }

        if (!deletes.isEmpty()) {
            postSearchRepository.deleteAllById(deletes);
            log.info("게시글 ES 삭제 반영 완료: {}건", deletes.size());
        }

        if (!upserts.isEmpty()) {
            postSearchRepository.saveAll(upserts);
            log.info("게시글 ES Bulk Indexing 완료: {}건", upserts.size());
        }
    }

    private LocalDate parseToDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDate.now();
        }
        try {
            // ISO 형식(LocalDateTime)에서 LocalDate만 추출
            return LocalDateTime.parse(dateStr).toLocalDate();
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}, 기본값(오늘) 사용", dateStr);
            return LocalDate.now();
        }
    }
}
