package com.bit.idol.voteservice.service;

import com.bit.idol.voteservice.entity.OutboxEvent;
import com.bit.idol.voteservice.repository.OutboxRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000) // 5초마다 미처리 이벤트 확인
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> events = outboxRepository.findByProcessedFalseOrderByCreatedAtAsc();
        
        for (OutboxEvent event : events) {
            try {
                processEvent(event);
                
                event.setProcessed(true);
                event.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(event);
                
                log.info("Outbox 이벤트 처리 완료: id={}, type={}", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("Outbox 이벤트 처리 실패: id={}, error={}", event.getId(), e.getMessage());
                // 실패한 경우 다음 주기에 재시도
            }
        }
    }

    private void processEvent(OutboxEvent event) throws Exception {
        if ("VOTE_CANCELLED".equals(event.getEventType())) {
            Map<String, Object> payload = objectMapper.readValue(event.getPayload(), new TypeReference<Map<String, Object>>() {});
            
            int voteId = ((Number) payload.get("voteId")).intValue();
            int userId = ((Number) payload.get("userId")).intValue();
            int candidateNumber = ((Number) payload.get("candidateNumber")).intValue();
            String redisKey = (String) payload.get("redisKey");
            String processedKey = (String) payload.get("processedKey");

            // 1. Kafka 전송 (랭킹 서비스 ZSET 차감용)
            String uuid = UUID.randomUUID().toString();
            String kafkaMessage = uuid + ":" + voteId + ":" + userId + ":-" + candidateNumber;
            kafkaTemplate.send("vote-complete-topic", kafkaMessage);

            // 2. Redis 키 삭제 (중복 투표 방지 해제)
            redisTemplate.delete(Arrays.asList(redisKey, processedKey));
            
            log.info("VOTE_CANCELLED 이벤트 외부 시스템 전파 완료: voteId={}, userId={}", voteId, userId);
        } else if ("VOTE_CAST".equals(event.getEventType())) {
            Map<String, Object> payload = objectMapper.readValue(event.getPayload(), new TypeReference<Map<String, Object>>() {});
            
            String uuid = (String) payload.get("uuid");
            int voteId = ((Number) payload.get("voteId")).intValue();
            int userId = ((Number) payload.get("userId")).intValue();
            int candidateNumber = ((Number) payload.get("candidateNumber")).intValue();

            // Kafka 전송 (vote-service-topic -> VoteConsumer가 수신)
            String kafkaMessage = uuid + ":" + voteId + ":" + userId + ":" + candidateNumber;
            kafkaTemplate.send("vote-service-topic", kafkaMessage);
            
            log.info("VOTE_CAST 이벤트 외부 시스템 전파 완료: voteId={}, userId={}", voteId, userId);
        }
    }
}
