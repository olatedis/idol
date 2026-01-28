package com.bit.idol.voteservice.service;

import com.bit.idol.voteservice.client.UserFeignClient;
import com.bit.idol.voteservice.dto.MyVoteRecordDto;
import com.bit.idol.voteservice.dto.UserDto;
import com.bit.idol.voteservice.dto.VoteInfo;
import com.bit.idol.voteservice.dto.notification.NotificationEventDto;
import com.bit.idol.voteservice.dto.notification.TargetType;
import com.bit.idol.voteservice.entity.Vote;
import com.bit.idol.voteservice.entity.VoteRecord;
import com.bit.idol.voteservice.producer.NotificationProducer;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final VoteRepository voteRepository;
    private final VoteReader voteReader;
    private final VoteRecordRepository voteRecordRepository;
    private final CandidateRepository candidateRepository;
    private final NotificationProducer notificationProducer;
    private final UserFeignClient userFeignClient;

    @Transactional
    @CachePut(value = "voteInfo", key = "#result.id")
    public VoteInfo createVote(Vote vote) {
        Vote savedVote = voteRepository.save(vote);
        
        TargetType targetType = TargetType.ALL;
        String targetId = null;
        
        if (savedVote.getTargetGroupId() != null) {
            targetType = TargetType.GROUP_SUB;
            targetId = String.valueOf(savedVote.getTargetGroupId());
        }

        sendVoteNotification(savedVote, "VOTE_OPENED", targetType, targetId);
        
        return VoteInfo.from(savedVote);
    }

    @Transactional
    @CircuitBreaker(name = "redis-vote", fallbackMethod = "castVoteFallback")
    public String castVote(int voteId, int userId, int candidateNumber, String clientIp) {
        validateIp(clientIp);

        VoteInfo vote = voteReader.getVoteInfo(voteId);
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(vote.getStartDate())) {
            throw new RuntimeException("투표가 아직 시작되지 않았습니다.");
        }

        if (now.isAfter(vote.getEndDate())) {
            throw new RuntimeException("투표가 이미 종료되었습니다.");
        }

        // ★ 뉴비 차단 (Newbie Ban) 로직 추가
        try {
            UserDto user = userFeignClient.getUserInfoById(userId);
            if (user != null && user.getCreatedAt() != null) {
                if (user.getCreatedAt().isAfter(vote.getStartDate())) {
                    throw new RuntimeException("투표 기간 중 가입한 계정은 참여할 수 없습니다.");
                }
            }
        } catch (Exception e) {
            log.warn("유저 정보 조회 실패 (뉴비 체크 건너뜀): {}", e.getMessage());
            // 유저 서비스 장애 시에는 투표를 막을지, 허용할지 정책 결정 필요
            // 여기서는 일단 허용 (Fail-Open)
        }

        String redisKey = "vote:" + voteId + ":user:" + userId;
        Duration ttl = Duration.between(now, vote.getEndDate());

        Boolean isVoted = redisTemplate.opsForValue().setIfAbsent(redisKey, "voted", ttl);

        if (Boolean.FALSE.equals(isVoted)) {
            throw new RuntimeException("이미 투표에 참여하였습니다.");
        }

        sendToKafka(voteId, userId, candidateNumber, redisKey);

        sendVoteNotification(null, "VOTE_COMPLETED", TargetType.USER, String.valueOf(userId));

        return "투표가 완료되었습니다.";
    }

    public String castVoteFallback(int voteId, int userId, int candidateNumber, String clientIp, Throwable t) {
        log.warn("Redis 장애 감지! DB 기반 투표로 전환합니다. Error: {}", t.getMessage());

        VoteInfo vote = voteReader.getVoteInfo(voteId);
        
        if (voteRecordRepository.findByVoteIdAndUserId(voteId, userId).isPresent()) {
            throw new RuntimeException("이미 투표에 참여하였습니다. (DB Check)");
        }

        try {
            String uuid = UUID.randomUUID().toString();
            String message = uuid + ":" + voteId + ":" + userId + ":" + candidateNumber;
            kafkaTemplate.send("vote-topic", message);
        } catch (Exception e) {
            throw new RuntimeException("투표 시스템 장애로 인해 투표를 처리할 수 없습니다.", e);
        }

        return "투표가 완료되었습니다. (지연 처리)";
    }

    private void sendToKafka(int voteId, int userId, int candidateNumber, String redisKey) {
        String uuid = UUID.randomUUID().toString();
        String message = uuid + ":" + voteId + ":" + userId + ":" + candidateNumber;

        try {
            kafkaTemplate.send("vote-topic", message);
        } catch (Exception e) {
            redisTemplate.delete(redisKey);
            throw new RuntimeException("투표 전송 중 오류가 발생했습니다. 다시 시도해주세요.", e);
        }
    }

    private void validateIp(String ipAddress) {
        // Level 1: IDC/VPN IP 차단 (예시 대역)
        if (ipAddress.startsWith("10.") || ipAddress.startsWith("192.168.")) {
            // 내부망 IP는 허용 (테스트용)
        } else if (isBlacklistedIp(ipAddress)) {
            throw new RuntimeException("비정상적인 접근입니다. (VPN/Proxy 차단)");
        }

        String key = "vote:limit:ip:" + ipAddress;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        if (count != null && count > 10) {
            throw new RuntimeException("비정상적인 투표 시도가 감지되었습니다. 잠시 후 다시 시도해주세요.");
        }
    }
    
    private boolean isBlacklistedIp(String ip) {
        // 실제로는 DB나 Redis에서 블랙리스트 조회
        // 여기서는 예시로 AWS 대역 일부 차단
        return ip.startsWith("3.5.") || ip.startsWith("13.124."); 
    }

    @Transactional
    public void cancelVote(int voteId, int userId) {
        VoteRecord record = voteRecordRepository.findByVoteIdAndUserId(voteId, userId)
                .orElseThrow(() -> new RuntimeException("투표 이력이 없습니다."));

        VoteInfo vote = voteReader.getVoteInfo(voteId);

        if (LocalDateTime.now().isAfter(vote.getEndDate())) {
            throw new RuntimeException("이미 종료된 투표는 취소할 수 없습니다.");
        }

        voteRecordRepository.delete(record);
        candidateRepository.decrementVoteCount(record.getCandidateId());

        try {
            String redisKey = "vote:" + voteId + ":user:" + userId;
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.error("Redis 키 삭제 실패 (투표 취소): {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<MyVoteRecordDto> getMyVoteRecords(int userId) {
        return voteRecordRepository.findMyVoteRecords(userId);
    }

    private void sendVoteNotification(Vote vote, String type, TargetType targetType, String targetId) {
        try {
            Map<String, String> args = new HashMap<>();
            
            String redirectUrl = "/vote";
            if (vote != null) {
                args.put("voteTitle", vote.getTitle());
                redirectUrl = "/vote/" + vote.getId();
            }

            NotificationEventDto event = NotificationEventDto.builder()
                    .eventId(UUID.randomUUID().toString())
                    .type(type)
                    .targetType(targetType)
                    .targetId(targetId)
                    .args(args)
                    .redirectUrl(redirectUrl)
                    .occurredAt(LocalDateTime.now())
                    .build();
            
            notificationProducer.send(event);
        } catch (Exception e) {
            log.error("알림 발송 실패: {}", e.getMessage());
        }
    }
}
