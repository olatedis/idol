package com.bit.idol.rankingservice.consumer;

import com.bit.idol.rankingservice.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RankingConsumer {

    private final RankingService rankingService;

    // 투표 완료 메시지 수신 (vote-service에서 보냄)
    // 메시지 포맷: "voteId:userId:candidateNumber"
    @KafkaListener(topics = "vote-complete-topic", groupId = "ranking-group")
    public void consumeVoteComplete(String message) {
        log.info("랭킹 서비스 - 투표 완료 메시지 수신: {}", message);

        try {
            String[] parts = message.split(":");
            int voteId = Integer.parseInt(parts[0]);
            // userId는 랭킹 집계에 필요 없음
            int candidateNumber = Integer.parseInt(parts[2]);

            // Redis ZSET 점수 업데이트 및 실시간 전파
            rankingService.updateRanking(voteId, candidateNumber);

        } catch (Exception e) {
            log.error("랭킹 업데이트 실패: message={}", message, e);
        }
    }
}
