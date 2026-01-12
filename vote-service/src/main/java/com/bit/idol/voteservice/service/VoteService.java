package com.bit.idol.voteservice.service;

import com.bit.idol.voteservice.dto.VoteInfo;
import com.bit.idol.voteservice.entity.Vote;
import com.bit.idol.voteservice.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final VoteRepository voteRepository;
    private final VoteReader voteReader; // 분리된 Reader 주입

    // 투표 생성 (Cache Warming 적용)
    @Transactional
    @CachePut(value = "voteInfo", key = "#result.id")
    public VoteInfo createVote(Vote vote) {
        Vote savedVote = voteRepository.save(vote);
        return VoteInfo.from(savedVote);
    }

    // 투표 참여
    @Transactional
    public String castVote(int voteId, int userId, int candidateNumber) {

        // 1. 투표 정보 조회 (캐시 사용 - VoteReader를 사용한 이유는 Cacheable은 내부에서 동작안함 외부로 빼야 동작)
        VoteInfo vote = voteReader.getVoteInfo(voteId);

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(vote.getStartDate())) {
            throw new RuntimeException("투표가 아직 시작되지 않았습니다.");
        }

        if (now.isAfter(vote.getEndDate())) {
            throw new RuntimeException("투표가 이미 종료되었습니다.");
        }

        // 2. Redis 키 생성 및 TTL 계산 (종료 시간까지만 유지)
        String redisKey = "vote:" + voteId + ":user:" + userId;
        Duration ttl = Duration.between(now, vote.getEndDate());

        // 3. Redis를 통한 중복 체크
        Boolean isVoted = redisTemplate.opsForValue().setIfAbsent(redisKey, "voted", ttl);

        if (Boolean.FALSE.equals(isVoted)) {
            throw new RuntimeException("이미 투표에 참여하였습니다.");
        }

        // 4. 카프카로 메세지 전송
        String message = voteId + ":" + userId + ":" + candidateNumber;

        try {
            kafkaTemplate.send("vote-topic", message);
        } catch (Exception e) {
            // Kafka 전송 실패 시 Redis 키 삭제 (보상 트랜잭션)
            redisTemplate.delete(redisKey);
            throw new RuntimeException("투표 전송 중 오류가 발생했습니다. 다시 시도해주세요.", e);
        }

        return "투표가 완료되었습니다.";
    }
}
