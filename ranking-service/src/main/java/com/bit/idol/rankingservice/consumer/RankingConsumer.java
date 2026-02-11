package com.bit.idol.rankingservice.consumer;

import com.bit.idol.rankingservice.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RankingConsumer {

    private final RankingService rankingService;
    private final RedisTemplate<String, String> redisTemplate;

    // 투표 완료 메시지 수신 (vote-service에서 보냄)
    // 메시지 포맷: "uuid:voteId:userId:candidateNumber"
    @KafkaListener(topics = "${spring.kafka.topic.vote-complete}", groupId = "ranking-group")
    public void consumeVoteComplete(String message) {
        log.info("랭킹 서비스 - 투표 완료 메시지 수신: {}", message);

        try {
            String[] parts = message.split(":");
            if (parts.length < 4) {
                log.error("잘못된 메시지 형식: {}", message);
                return;
            }

            String uuid = parts[0];
            int voteId = Integer.parseInt(parts[1]);
            // userId는 랭킹 집계에 필요 없음
            int candidateNumber = Integer.parseInt(parts[3]);

            // 1. 멱등성 검사 (Redis 중복 체크)
            String processedKey = "processed:ranking-msg:" + uuid;
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(processedKey, "1", Duration.ofMinutes(10));

            if (Boolean.FALSE.equals(isNew)) {
                log.warn("중복된 랭킹 메시지 감지 (처리 건너뜀): uuid={}", uuid);
                return;
            }

            // 2. Redis ZSET 점수 업데이트
            rankingService.updateRanking(voteId, candidateNumber);

        } catch (Exception e) {
            log.error("랭킹 업데이트 실패: message={}", message, e);
        }
    }
}
