package com.bit.idol.rankingservice.service;

import com.bit.idol.rankingservice.dto.RankingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket 전송 도구

    // 랭킹 업데이트
    public void updateRanking(int voteId, int candidateNumber) {
        String key = "vote:ranking:" + voteId;
        String member = String.valueOf(candidateNumber);

        // 1. Redis ZSET 점수 증가 (Score + 1)
        redisTemplate.opsForZSet().incrementScore(key, member, 1);
        
        log.info("랭킹 업데이트 완료: voteId={}, candidate={}, score=+1", voteId, candidateNumber);

        // 2. 랭킹 정보 조회 (전체 순위)
        // 0부터 -1까지 조회 (전체)
        Set<ZSetOperations.TypedTuple<String>> allRankings = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);
        
        if (allRankings == null) return;

        List<RankingDto> rankingList = allRankings.stream()
                .map(tuple -> new RankingDto(Integer.parseInt(tuple.getValue()), tuple.getScore().intValue()))
                .collect(Collectors.toList());

        // 3. WebSocket으로 구독자들에게 전송
        // 경로: /topic/votes/{voteId}/ranking
        String destination = "/topic/votes/" + voteId + "/ranking";
        messagingTemplate.convertAndSend(destination, rankingList);
    }
}
