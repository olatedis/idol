package com.bit.idol.rankingservice.service;

import com.bit.idol.rankingservice.dto.RankingDto;
import com.bit.idol.rankingservice.dto.notification.NotificationEventDto;
import com.bit.idol.rankingservice.dto.notification.TargetType;
import com.bit.idol.rankingservice.producer.NotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationProducer notificationProducer;

    // 랭킹 업데이트 (Kafka Consumer가 호출)
    public void updateRanking(int voteId, int candidateNumber, int scoreDelta) {
        String key = "vote:ranking:" + voteId;
        String member = String.valueOf(candidateNumber);

        // 1. Redis ZSET 점수 증감
        redisTemplate.opsForZSet().incrementScore(key, member, scoreDelta);

        // 2. 활성 투표 목록에 추가 (스케줄러가 확인하도록)
        redisTemplate.opsForSet().add("vote:active-list", String.valueOf(voteId));

        log.info("랭킹 점수 반영 완료: voteId={}, candidate={}, delta={}", voteId, candidateNumber, scoreDelta);
    }

    // 랭킹 조회 (API 호출용 - Delta 계산 없음)
    public List<RankingDto> getRanking(int voteId) {
        String key = "vote:ranking:" + voteId;

        Set<ZSetOperations.TypedTuple<String>> allRankings = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0,
                -1);

        if (allRankings == null || allRankings.isEmpty()) {
            return Collections.emptyList();
        }

        return allRankings.stream()
                .map(tuple -> new RankingDto(Integer.parseInt(tuple.getValue()), tuple.getScore().intValue(), 0)) // 초기
                                                                                                                  // 조회
                                                                                                                  // 시
                                                                                                                  // Delta는
                                                                                                                  // 0
                .collect(Collectors.toList());
    }

    // 랭킹 전송 (스케줄러가 호출 - Delta 계산 포함)
    public void broadcastRanking(int voteId) {
        String key = "vote:ranking:" + voteId;
        String prevScoreKey = "vote:ranking:prev:" + voteId;

        // 1. 현재 랭킹 조회
        Set<ZSetOperations.TypedTuple<String>> allRankings = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0,
                -1);

        if (allRankings == null || allRankings.isEmpty())
            return;

        // 2. 이전 점수 조회 (Redis Hash)
        // Key: candidateNumber, Value: score
        Map<Object, Object> prevScores = redisTemplate.opsForHash().entries(prevScoreKey);

        List<RankingDto> rankingList = new ArrayList<>();
        Map<String, String> currentScoresToSave = new HashMap<>();

        for (ZSetOperations.TypedTuple<String> tuple : allRankings) {
            int candidateNum = Integer.parseInt(tuple.getValue());
            int currentScore = tuple.getScore().intValue();

            // 이전 점수 가져오기 (없으면 0)
            int prevScore = 0;
            if (prevScores.containsKey(String.valueOf(candidateNum))) {
                prevScore = Integer.parseInt((String) prevScores.get(String.valueOf(candidateNum)));
            }

            // Delta 계산
            int delta = currentScore - prevScore;

            rankingList.add(new RankingDto(candidateNum, currentScore, delta));

            // 다음 계산을 위해 현재 점수 저장 준비
            currentScoresToSave.put(String.valueOf(candidateNum), String.valueOf(currentScore));
        }

        // 3. 현재 점수를 Redis에 저장 (다음 1초 뒤 비교용)
        if (!currentScoresToSave.isEmpty()) {
            redisTemplate.opsForHash().putAll(prevScoreKey, currentScoresToSave);
        }

        // 4. WebSocket 전송
        String destination = "/topic/votes/" + voteId + "/ranking";
        messagingTemplate.convertAndSend(destination, rankingList);

        // 5. 1위 변경 시 알림 발행
        if (!rankingList.isEmpty()) {
            String prevLeaderKey = "vote:ranking:prev-leader:" + voteId;
            String currentLeader = String.valueOf(rankingList.get(0).getCandidateNumber());
            String prevLeader = redisTemplate.opsForValue().get(prevLeaderKey);

            if (prevLeader != null && !prevLeader.equals(currentLeader)) {
                try {
                    // voteId로 vote 제목 조회(없으면 ID로 대체)
                    String voteTitle = redisTemplate.opsForValue().get("vote:title:" + voteId);
                    if (voteTitle == null) voteTitle = "투표 #" + voteId;

                    NotificationEventDto event = NotificationEventDto.builder()
                            .eventId(java.util.UUID.randomUUID().toString())
                            .type("RANKING_CHANGED")
                            .targetType(TargetType.GROUP_SUB)
                            .targetId(redisTemplate.opsForValue().get("vote:group:" + voteId))
                            .args(java.util.Map.of(
                                    "voteTitle", voteTitle,
                                    "newLeaderName", currentLeader,
                                    "prevLeaderName", prevLeader
                            ))
                            .redirectUrl("/vote/" + voteId)
                            .occurredAt(java.time.LocalDateTime.now())
                            .build();
                    notificationProducer.send(event);
                    log.info("랜킹 1위 변경 알림 발행: voteId={}, prev={}, new={}", voteId, prevLeader, currentLeader);
                } catch (Exception e) {
                    log.error("랜킹 변경 알림 발행 실패: {}", e.getMessage());
                }
            }

            if (currentLeader != null) {
                redisTemplate.opsForValue().set(prevLeaderKey, currentLeader);
            }
        }
    }
}
