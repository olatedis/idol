package com.bit.idol.voteservice.service;

import com.bit.idol.voteservice.client.UserFeignClient;
import com.bit.idol.voteservice.dto.MyVoteRecordDto;
import com.bit.idol.voteservice.dto.UserDto;
import com.bit.idol.voteservice.dto.VoteInfo;
import com.bit.idol.voteservice.dto.VoteListDto;
import com.bit.idol.voteservice.dto.notification.NotificationEventDto;
import com.bit.idol.voteservice.dto.notification.TargetType;
import com.bit.idol.voteservice.entity.Vote;
import com.bit.idol.voteservice.entity.VoteRecord;
import com.bit.idol.voteservice.producer.NotificationProducer;
import com.bit.idol.voteservice.repository.CandidateRepository;
import com.bit.idol.voteservice.repository.VoteRecordRepository;
import com.bit.idol.voteservice.repository.VoteRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    private static final String BLACKLIST_KEY = "vote:blacklist:ip";

    // 투표 목록 조회 (프론트엔드용)
    @Transactional(readOnly = true)
    public List<VoteListDto> getVoteList(int userId) {
        // 1. 모든 투표 조회 (VoteReader를 통해 캐싱 적용)
        List<Vote> votes = voteReader.getAllVotesCached();

        // 2. 내가 참여한 투표 ID 목록 조회
        List<Integer> myVotedVoteIds = voteRecordRepository.findVoteIdsByUserId(userId);
        Set<Integer> myVotedSet = new HashSet<>(myVotedVoteIds);

        // 3. DTO 변환
        return votes.stream().map(vote -> {
            String status = "PROGRESS";
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(vote.getStartDate())) {
                status = "UPCOMING";
            } else if (now.isAfter(vote.getEndDate())) {
                status = "ENDED";
            }

            return VoteListDto.builder()
                    .id((long) vote.getId())
                    .title(vote.getTitle())
                    .description(vote.getDescription())
                    .startDate(vote.getStartDate())
                    .endDate(vote.getEndDate())
                    .status(status)
                    .participantCount(vote.getTotalVotes()) // totalVotes 필드 사용 (성능 최적화)
                    .isVoted(myVotedSet.contains(vote.getId()))
                    .thumbnailUrl(null) // 썸네일 URL 필드 추가 필요
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    @CachePut(value = "voteInfo", key = "#result.id")
    @CacheEvict(value = "votes", key = "'all'") // 투표 생성 시 전체 목록 캐시 삭제
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

        // 뉴비 차단 (Newbie Ban)
        try {
            UserDto user = userFeignClient.getUserInfoById(userId);
            if (user != null && user.getCreatedAt() != null) {
                if (user.getCreatedAt().isAfter(vote.getStartDate())) {
                    throw new RuntimeException("투표 기간 중 가입한 계정은 참여할 수 없습니다.");
                }
            }
        } catch (Exception e) {
            log.warn("유저 정보 조회 실패 (뉴비 체크 건너뜀): {}", e.getMessage());
        }

        String redisKey = "vote:" + voteId + ":user:" + userId;
        Duration ttl = Duration.between(now, vote.getEndDate());

        Boolean isVoted = redisTemplate.opsForValue().setIfAbsent(redisKey, "voted", ttl);

        if (Boolean.FALSE.equals(isVoted)) {
            throw new RuntimeException("이미 투표에 참여하였습니다.");
        }

        try {
            sendToKafka(voteId, userId, candidateNumber, redisKey);
            sendVoteNotification(null, "VOTE_COMPLETED", TargetType.USER, String.valueOf(userId));
        } catch (Exception e) {
            // Kafka 전송 실패 시 Redis 키 삭제 (보상 트랜잭션)
            redisTemplate.delete(redisKey);
            throw e;
        }

        return "투표가 완료되었습니다.";
    }

    // Redis 장애 시 실행되는 Fallback 메서드
    // DB 보호를 위해 RateLimiter 적용 (초당 500건 제한)
    @RateLimiter(name = "vote-db-protection", fallbackMethod = "rateLimitFallback")
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

    // RateLimiter에 걸렸을 때 실행되는 Fallback
    public String rateLimitFallback(int voteId, int userId, int candidateNumber, String clientIp, RequestNotPermitted t) {
        log.error("DB 보호를 위해 투표 요청 거절: userId={}", userId);
        throw new RuntimeException("현재 투표량이 많아 잠시 후 다시 시도해주세요.");
    }
    
    // 그 외 예외에 대한 Fallback (RateLimiter 서명과 맞추기 위해 필요할 수 있음)
    public String rateLimitFallback(int voteId, int userId, int candidateNumber, String clientIp, Throwable t) {
        if (t instanceof RequestNotPermitted) {
            return rateLimitFallback(voteId, userId, candidateNumber, clientIp, (RequestNotPermitted) t);
        }
        // 원래 예외 다시 던지기
        throw new RuntimeException(t);
    }

    private void sendToKafka(int voteId, int userId, int candidateNumber, String redisKey) {
        String uuid = UUID.randomUUID().toString();
        String message = uuid + ":" + voteId + ":" + userId + ":" + candidateNumber;

        // 여기서 예외 발생 시 상위 메서드(castVote)에서 catch하여 Redis 키 삭제함
        kafkaTemplate.send("vote-topic", message);
    }

    private void validateIp(String ipAddress) {
        // Redis 기반 블랙리스트 확인
        if (isBlacklistedIp(ipAddress)) {
            throw new RuntimeException("비정상적인 접근입니다. (관리자에 의해 차단된 IP)");
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
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(BLACKLIST_KEY, ip));
    }

    // --- 블랙리스트 관리 메서드 ---

    public void addBlacklistIp(String ip) {
        redisTemplate.opsForSet().add(BLACKLIST_KEY, ip);
        log.info("IP 블랙리스트 추가: {}", ip);
    }

    public void removeBlacklistIp(String ip) {
        redisTemplate.opsForSet().remove(BLACKLIST_KEY, ip);
        log.info("IP 블랙리스트 해제: {}", ip);
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
