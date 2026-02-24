package com.bit.idol.voteservice.service;

import com.bit.idol.voteservice.client.UserFeignClient;
import com.bit.idol.voteservice.dto.MyVoteRecordDto;
import com.bit.idol.voteservice.dto.UserDto;
import com.bit.idol.voteservice.dto.VoteInfo;
import com.bit.idol.voteservice.dto.VoteListDto;
import com.bit.idol.voteservice.dto.event.VoteEvent;
import com.bit.idol.voteservice.dto.notification.TargetType;
import com.bit.idol.voteservice.entity.Vote;
import com.bit.idol.voteservice.entity.VoteRecord;
import com.bit.idol.voteservice.entity.VoteStatus;
import com.bit.idol.voteservice.repository.CandidateRepository;
import com.bit.idol.voteservice.repository.VoteRecordRepository;
import com.bit.idol.voteservice.repository.VoteRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final UserFeignClient userFeignClient;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${spring.kafka.topic.vote}")
    private String voteTopic;

    private static final String BLACKLIST_KEY = "vote:blacklist:ip";

    private static final String VOTE_SCRIPT = "if redis.call('EXISTS', KEYS[1]) == 1 then " +
            "   return 0 " +
            "end " +
            "redis.call('SET', KEYS[1], 'voted') " +
            "redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
            "return 1";

    @Transactional(readOnly = true)
    public List<VoteListDto> getVoteList(int userId, Long groupId) {
        List<Vote> votes = voteReader.getAllVotesCached();
        List<Integer> myVotedVoteIds = voteRecordRepository.findVoteIdsByUserId(userId);
        Set<Integer> myVotedSet = new HashSet<>(myVotedVoteIds);

        return votes.stream()
                .filter(vote -> groupId == null || groupId.equals(vote.getTargetGroupId())) // 그룹 필터 적용
                .map(vote -> {
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
                            .participantCount(vote.getTotalVotes())
                            .isVoted(myVotedSet.contains(vote.getId()))
                            .thumbnailUrl(null)
                            .build();
                }).collect(Collectors.toList());
    }

    @Transactional
    @CachePut(value = "voteInfo", key = "#result.id")
    @CacheEvict(value = "votes", key = "'all'")
    public VoteInfo createVote(Vote vote) {
        Vote savedVote = voteRepository.save(vote);

        TargetType targetType = TargetType.ALL;
        String targetId = null;

        if (savedVote.getTargetGroupId() != null) {
            targetType = TargetType.GROUP_SUB;
            targetId = String.valueOf(savedVote.getTargetGroupId());
        }

        eventPublisher.publishEvent(new VoteEvent(savedVote, "VOTE_OPENED", targetType, targetId));

        return VoteInfo.from(savedVote);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = "votes", key = "'all'")
    public void closeVote(int voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new RuntimeException("투표를 찾을 수 없습니다."));

        vote.setStatus(VoteStatus.CLOSED);

        String rankingKey = "vote:ranking:" + vote.getId();
        String prevScoreKey = "vote:ranking:prev:" + vote.getId();

        // ZSET, 이전 점수 해시, 그리고 활성화 투표 목록에서 해당 voteId 제거
        redisTemplate.delete(rankingKey);
        redisTemplate.delete(prevScoreKey);
        redisTemplate.opsForSet().remove("vote:active-list", String.valueOf(vote.getId()));
        TargetType targetType = TargetType.ALL;
        String targetId = null;
        if (vote.getTargetGroupId() != null) {
            targetType = TargetType.GROUP_SUB;
            targetId = String.valueOf(vote.getTargetGroupId());
        }

        eventPublisher.publishEvent(new VoteEvent(vote, "VOTE_CLOSED", targetType, targetId));

        log.info("투표 종료 처리 완료: ID={}, 제목={}", vote.getId(), vote.getTitle());
    }

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

        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(VOTE_SCRIPT, Long.class),
                Collections.singletonList(redisKey),
                String.valueOf(ttl.getSeconds()));

        if (result == null || result == 0) {
            throw new RuntimeException("이미 투표에 참여하였습니다.");
        }

        try {
            sendToKafka(voteId, userId, candidateNumber, redisKey);
        } catch (Exception e) {
            redisTemplate.delete(redisKey);
            throw e;
        }

        return "투표가 완료되었습니다.";
    }

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
            kafkaTemplate.send(voteTopic, message);
        } catch (Exception e) {
            throw new RuntimeException("투표 시스템 장애로 인해 투표를 처리할 수 없습니다.", e);
        }

        return "투표가 완료되었습니다. (지연 처리)";
    }

    // RateLimiter Fallback 메서드 (파라미터 일치시킴)
    public String rateLimitFallback(int voteId, int userId, int candidateNumber, String clientIp, Throwable t) {
        if (t instanceof RequestNotPermitted) {
            log.error("DB 보호를 위해 투표 요청 거절: userId={}", userId);
            throw new RuntimeException("현재 투표량이 많아 잠시 후 다시 시도해주세요.");
        }
        // 다른 예외는 그대로 던짐
        throw new RuntimeException(t);
    }

    private void sendToKafka(int voteId, int userId, int candidateNumber, String redisKey) {
        String uuid = UUID.randomUUID().toString();
        String message = uuid + ":" + voteId + ":" + userId + ":" + candidateNumber;

        try {
            kafkaTemplate.send(voteTopic, message);
        } catch (Exception e) {
            redisTemplate.delete(redisKey);
            throw new RuntimeException("투표 전송 중 오류가 발생했습니다. 다시 시도해주세요.", e);
        }
    }

    private void validateIp(String ipAddress) {
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

        VoteInfo voteInfo = voteReader.getVoteInfo(voteId);

        if (LocalDateTime.now().isAfter(voteInfo.getEndDate())) {
            throw new RuntimeException("이미 종료된 투표는 취소할 수 없습니다.");
        }

        // DB 차감: 기록 삭제, 후보자 득표수 감소, 투표 전체 참여자수 감소
        voteRecordRepository.delete(record);
        voteRecordRepository.flush(); // 영속성 컨텍스트 즉시 DB 반영 (삭제 확실히 보장)

        candidateRepository.decrementVoteCount(record.getCandidateId());
        candidateRepository.flush();

        com.bit.idol.voteservice.entity.Vote voteEntity = voteRepository.findById(voteId)
                .orElseThrow(() -> new RuntimeException("투표를 찾을 수 없습니다."));
        if (voteEntity.getTotalVotes() > 0) {
            voteEntity.setTotalVotes(voteEntity.getTotalVotes() - 1);
            voteRepository.saveAndFlush(voteEntity);
        }

        // Kafka 전송: ranking-service가 ZSET에서 점수를 빼도록 마이너스 번호로 보냄
        com.bit.idol.voteservice.entity.Candidate candidate = candidateRepository.findById(record.getCandidateId())
                .orElseThrow(() -> new RuntimeException("후보자를 찾을 수 없습니다."));
        try {
            String uuid = UUID.randomUUID().toString();
            String message = uuid + ":" + voteId + ":" + userId + ":-" + candidate.getNumber();
            kafkaTemplate.send("vote-complete-topic", message);
        } catch (Exception e) {
            log.error("랭킹 서비스로 취소 이벤트 전송 실패: {}", e.getMessage());
        }

        // Redis 중복 방지 키 롤백
        try {
            String redisKey = "vote:" + voteId + ":user:" + userId;
            String processedKey = "processed:vote:" + voteId + ":user:" + userId;
            redisTemplate.delete(Arrays.asList(redisKey, processedKey));
        } catch (Exception e) {
            log.error("Redis 키 삭제 실패 (투표 취소): {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<MyVoteRecordDto> getMyVoteRecords(int userId) {
        return voteRecordRepository.findMyVoteRecords(userId);
    }
}
