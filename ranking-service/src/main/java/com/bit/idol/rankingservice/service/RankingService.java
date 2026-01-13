package com.bit.idol.rankingservice.service;

import com.bit.idol.rankingservice.dto.RankingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    // 랭킹 업데이트 (Kafka Consumer가 호출)
    public void updateRanking(int voteId, int candidateNumber) {
        String key = "vote:ranking:" + voteId;
        String member = String.valueOf(candidateNumber);

        // 1. Redis ZSET 점수 증가
        redisTemplate.opsForZSet().incrementScore(key, member, 1);
        
        // 2. 활성 투표 목록에 추가 (스케줄러가 확인하도록)
        redisTemplate.opsForSet().add("vote:active-list", String.valueOf(voteId));
        
        log.info("랭킹 점수 반영 완료: voteId={}, candidate={}", voteId, candidateNumber);
    }

    // 랭킹 조회 (API 호출용)
    public List<RankingDto> getRanking(int voteId) {
        String key = "vote:ranking:" + voteId;
        
        // 전체 순위 조회
        Set<ZSetOperations.TypedTuple<String>> allRankings = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);
        
        if (allRankings == null || allRankings.isEmpty()) {
            return Collections.emptyList();
        }

        return allRankings.stream()
                .map(tuple -> new RankingDto(Integer.parseInt(tuple.getValue()), tuple.getScore().intValue()))
                .collect(Collectors.toList());
    }

    // 랭킹 전송 (스케줄러가 호출)
    public void broadcastRanking(int voteId) {
        // getRanking 메서드 재사용
        List<RankingDto> rankingList = getRanking(voteId);
        
        if (rankingList.isEmpty()) return;

        // WebSocket 전송
        String destination = "/topic/votes/" + voteId + "/ranking";
        messagingTemplate.convertAndSend(destination, rankingList);
    }
}
