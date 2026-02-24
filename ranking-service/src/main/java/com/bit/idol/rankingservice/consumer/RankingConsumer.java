package com.bit.idol.rankingservice.consumer;

import com.bit.idol.rankingservice.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RankingConsumer {

    private final RankingService rankingService;
    private final RedisTemplate<String, String> redisTemplate;

    // 투표 완료 메시지 수신 (vote-service에서 보냄)
    // 메시지 포맷: "uuid:voteId:userId:candidateNumber"
    @KafkaListener(topics = "${spring.kafka.topic.vote-complete}", groupId = "ranking-group")
    public void consumeVoteComplete(List<String> messages) {
        log.info("랭킹 서비스 - 투표 완료 배치 메시지 수신: {}건", messages.size());

        for (String message : messages) {
            try {
                String[] parts = message.split(":");
                if (parts.length < 4) {
                    log.error("잘못된 메시지 형식: {}", message);
                    continue;
                }

                String uuid = parts[0];
                int voteId = Integer.parseInt(parts[1]);
                // userId는 랭킹 집계에 필요 없음
                int candidateNumberRaw = Integer.parseInt(parts[3]);

                int candidateNumber = Math.abs(candidateNumberRaw);
                int scoreDelta = candidateNumberRaw > 0 ? 1 : -1;

                // 1. 멱등성 검사 (Redis 중복 체크)
                String processedKey = "processed:ranking-msg:" + uuid;
                Boolean isNew = redisTemplate.opsForValue().setIfAbsent(processedKey, "1", Duration.ofMinutes(10));

                if (Boolean.FALSE.equals(isNew)) {
                    log.warn("중복된 랭킹 메시지 감지 (처리 건너뜀): uuid={}", uuid);
                    continue;
                }

                // 2. Redis ZSET 점수 업데이트
                rankingService.updateRanking(voteId, candidateNumber, scoreDelta);

            } catch (Exception e) {
                log.error("랭킹 업데이트 실패: message={}", message, e);
            }
        }
    }
}
