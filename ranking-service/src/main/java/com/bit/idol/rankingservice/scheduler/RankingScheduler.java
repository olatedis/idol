package com.bit.idol.rankingservice.scheduler;

import com.bit.idol.rankingservice.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class RankingScheduler {

    private final RankingService rankingService;
    private final RedisTemplate<String, String> redisTemplate;

    // 1초마다 실행 (1000ms)
    @Scheduled(fixedRate = 1000)
    public void broadcastRankings() {
        // 1. 현재 활성화된(투표가 발생한) 투표 목록 조회
        Set<String> activeVoteIds = redisTemplate.opsForSet().members("vote:active-list");

        if (activeVoteIds == null || activeVoteIds.isEmpty()) {
            return;
        }

        // 2. 각 투표방에 랭킹 정보 전송
        for (String voteIdStr : activeVoteIds) {
            try {
                int voteId = Integer.parseInt(voteIdStr);
                rankingService.broadcastRanking(voteId);
            } catch (NumberFormatException e) {
                log.error("잘못된 voteId 형식: {}", voteIdStr);
            } catch (Exception e) {
                log.error("랭킹 브로드캐스트 실패: voteId={}", voteIdStr, e);
            }
        }
    }
}
