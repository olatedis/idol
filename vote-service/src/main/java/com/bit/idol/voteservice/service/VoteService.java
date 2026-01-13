package com.bit.idol.voteservice.service;

import com.bit.idol.voteservice.dto.VoteInfo;
import com.bit.idol.voteservice.entity.Vote;
import com.bit.idol.voteservice.entity.VoteRecord;
import com.bit.idol.voteservice.repository.CandidateRepository;
import com.bit.idol.voteservice.repository.VoteRecordRepository;
import com.bit.idol.voteservice.repository.VoteRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final VoteRepository voteRepository;
    private final VoteReader voteReader; // 분리된 Reader 주입
    private final VoteRecordRepository voteRecordRepository;
    private final CandidateRepository candidateRepository;

    // 투표 생성 (Cache Warming 적용)
    @Transactional
    @CachePut(value = "voteInfo", key = "#result.id")
    public VoteInfo createVote(Vote vote) {
        Vote savedVote = voteRepository.save(vote);
        return VoteInfo.from(savedVote);
    }

    // 투표 참여 (서킷 브레이커 적용)
    @Transactional
    @CircuitBreaker(name = "redis-vote", fallbackMethod = "castVoteFallback")
    public String castVote(int voteId, int userId, int candidateNumber, String clientIp) {

        // 0. IP 기반 어뷰징 체크 (Redis 사용 - 장애 시 예외 발생하여 Fallback으로 이동)
        validateIp(clientIp);

        // 1. 투표 정보 조회
        VoteInfo vote = voteReader.getVoteInfo(voteId);

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(vote.getStartDate())) {
            throw new RuntimeException("투표가 아직 시작되지 않았습니다.");
        }

        if (now.isAfter(vote.getEndDate())) {
            throw new RuntimeException("투표가 이미 종료되었습니다.");
        }

        // 2. Redis 키 생성 및 TTL 계산
        String redisKey = "vote:" + voteId + ":user:" + userId;
        Duration ttl = Duration.between(now, vote.getEndDate());

        // 3. Redis를 통한 중복 체크
        Boolean isVoted = redisTemplate.opsForValue().setIfAbsent(redisKey, "voted", ttl);

        if (Boolean.FALSE.equals(isVoted)) {
            throw new RuntimeException("이미 투표에 참여하였습니다.");
        }

        // 4. 카프카로 메세지 전송
        sendToKafka(voteId, userId, candidateNumber, redisKey);

        return "투표가 완료되었습니다.";
    }

    // Fallback 메서드 (Redis 장애 시 실행)
    public String castVoteFallback(int voteId, int userId, int candidateNumber, String clientIp, Throwable t) {
        log.warn("Redis 장애 감지! DB 기반 투표로 전환합니다. Error: {}", t.getMessage());

        // 1. 투표 정보 조회 (DB에서 직접 조회하거나 로컬 캐시 사용)
        // 여기서는 voteReader가 캐시를 쓰지만, Redis가 죽었으므로 DB로 갈 것임
        VoteInfo vote = voteReader.getVoteInfo(voteId);
        
        // 2. DB에서 중복 투표 체크
        if (voteRecordRepository.findByVoteIdAndUserId(voteId, userId).isPresent()) {
            throw new RuntimeException("이미 투표에 참여하였습니다. (DB Check)");
        }

        // 3. 카프카 전송
        try {
            String message = voteId + ":" + userId + ":" + candidateNumber;
            kafkaTemplate.send("vote-topic", message);
        } catch (Exception e) {
            throw new RuntimeException("투표 시스템 장애로 인해 투표를 처리할 수 없습니다.", e);
        }

        return "투표가 완료되었습니다. (지연 처리)";
    }

    private void sendToKafka(int voteId, int userId, int candidateNumber, String redisKey) {
        String message = voteId + ":" + userId + ":" + candidateNumber;

        try {
            kafkaTemplate.send("vote-topic", message);
        } catch (Exception e) {
            // Kafka 전송 실패 시 Redis 키 삭제 (보상 트랜잭션)
            redisTemplate.delete(redisKey);
            throw new RuntimeException("투표 전송 중 오류가 발생했습니다. 다시 시도해주세요.", e);
        }
    }

    // IP 검증 로직
    private void validateIp(String ipAddress) {
        String key = "vote:limit:ip:" + ipAddress;

        // 카운트 증가
        Long count = redisTemplate.opsForValue().increment(key);

        // 처음 요청이면 만료 시간 설정 (1분)
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        // 제한 초과 체크 (1분에 10회 이상)
        if (count != null && count > 10) {
            throw new RuntimeException("비정상적인 투표 시도가 감지되었습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    // 투표 취소
    @Transactional
    public void cancelVote(int voteId, int userId) {
        // 1. 투표 기록 조회
        VoteRecord record = voteRecordRepository.findByVoteIdAndUserId(voteId, userId)
                .orElseThrow(() -> new RuntimeException("투표 이력이 없습니다."));

        // 2. 투표 기간 체크
        VoteInfo vote = voteReader.getVoteInfo(voteId);

        if (LocalDateTime.now().isAfter(vote.getEndDate())) {
            throw new RuntimeException("이미 종료된 투표는 취소할 수 없습니다.");
        }

        // 3. DB 삭제 및 득표수 감소
        voteRecordRepository.delete(record);
        candidateRepository.decrementVoteCount(record.getCandidateId());

        // 4. Redis 키 삭제 (재투표 가능하게)
        try {
            String redisKey = "vote:" + voteId + ":user:" + userId;
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.error("Redis 키 삭제 실패 (투표 취소): {}", e.getMessage());
            // Redis가 죽어도 DB 취소는 성공해야 하므로 예외를 던지지 않음
        }
    }
}
