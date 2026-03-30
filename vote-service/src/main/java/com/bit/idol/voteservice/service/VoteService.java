package com.bit.idol.voteservice.service;

import com.bit.idol.voteservice.client.UserFeignClient;
import com.bit.idol.voteservice.dto.MyVoteRecordDto;
import com.bit.idol.voteservice.dto.UserDto;
import com.bit.idol.voteservice.dto.VoteInfo;
import com.bit.idol.voteservice.dto.VoteListDto;
import com.bit.idol.voteservice.dto.event.VoteEvent;
import com.bit.idol.voteservice.dto.notification.NotificationEventDto;
import com.bit.idol.voteservice.dto.notification.TargetType;
import com.bit.idol.voteservice.entity.OutboxEvent;
import com.bit.idol.voteservice.entity.Vote;
import com.bit.idol.voteservice.entity.VoteRecord;
import com.bit.idol.voteservice.entity.VoteStatus;
import com.bit.idol.voteservice.producer.NotificationProducer;
import com.bit.idol.voteservice.repository.CandidateRepository;
import com.bit.idol.voteservice.repository.OutboxRepository;
import com.bit.idol.voteservice.repository.VoteRecordRepository;
import com.bit.idol.voteservice.repository.VoteRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final NotificationProducer notificationProducer;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

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
        List<VoteInfo> votes = voteReader.getAllVotesCached();
        List<Integer> myVotedVoteIds = voteRecordRepository.findVoteIdsByUserId(userId);
        Set<Integer> myVotedSet = new HashSet<>(myVotedVoteIds);

        return votes.stream()
                .filter(vote -> groupId == null || java.util.Objects.equals(groupId, vote.getTargetGroupId()))
                .map(vote -> {
                    String status = vote.getStatus(); // VoteInfo에는 문자열 status가 이미 있음

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

        // RankingService 동기화용 Redis 키 설정
        try {
            redisTemplate.opsForValue().set("vote:title:" + savedVote.getId(), savedVote.getTitle(), Duration.ofDays(7));
            if (savedVote.getTargetGroupId() != null) {
                redisTemplate.opsForValue().set("vote:group:" + savedVote.getId(), String.valueOf(savedVote.getTargetGroupId()), Duration.ofDays(7));
            }
        } catch (Exception e) {
            log.error("RankingService 동기화 Redis 키 설정 실패: {}", e.getMessage());
        }

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

        // 1위 후보자 존재 시 VOTE_RESULT 알림 발행
        try {
            String topCandidate = redisTemplate.opsForZSet().reverseRange(rankingKey, 0, 0) != null
                    ? redisTemplate.opsForZSet().reverseRange(rankingKey, 0, 0).stream().findFirst().orElse(null)
                    : null;
            if (topCandidate != null) {
                TargetType targetType2 = vote.getTargetGroupId() != null ? TargetType.GROUP_SUB : TargetType.ALL;
                String targetId2 = vote.getTargetGroupId() != null ? String.valueOf(vote.getTargetGroupId()) : null;
                // 수정: 투표 알림 공통 규칙 적용
                Map<String, String> voteResultArgs = new HashMap<>();
                voteResultArgs.put("voteTitle", vote.getTitle());
                voteResultArgs.put("winnerName", "후보 " + topCandidate);

                if (vote.getTargetGroupId() != null) {
                    voteResultArgs.put("groupId", String.valueOf(vote.getTargetGroupId()));
                }

                String voteResultRedirectUrl = "/group/" + vote.getTargetGroupId() + "/vote";

                NotificationEventDto voteResultEvent = NotificationEventDto.builder()
                        .eventId(java.util.UUID.randomUUID().toString())
                        .type("VOTE_RESULT")
                        .targetType(targetType2)
                        .targetId(targetId2)
                        .args(voteResultArgs)
                        .redirectUrl(voteResultRedirectUrl)
                        .occurredAt(java.time.LocalDateTime.now())
                        .build();
                notificationProducer.send(voteResultEvent);
            }
        } catch (Exception e) {
            log.error("VOTE_RESULT 알림 발행 실패: {}", e.getMessage());
        }

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
    @Transactional
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
            saveToOutbox(voteId, userId, candidateNumber, redisKey);
        } catch (Exception e) {
            redisTemplate.delete(redisKey);
            throw e;
        }

        // 투표 완료 알림 발행
        try {
            VoteInfo voteInfo2 = voteReader.getVoteInfo(voteId);
            String candidateName = candidateRepository.findByVoteIdAndNumber(voteId, candidateNumber)
                    .map(c -> c.getName()).orElse("후보 " + candidateNumber);
            // 수정: 투표 알림 공통 규칙 적용
            Map<String, String> submitArgs = new HashMap<>();
            submitArgs.put("voteTitle", voteInfo2.getTitle());
            submitArgs.put("candidateName", candidateName);

            if (voteInfo2.getTargetGroupId() != null) {
                submitArgs.put("groupId", String.valueOf(voteInfo2.getTargetGroupId()));
            }

            String submitRedirectUrl = "/group/" + voteInfo2.getTargetGroupId() + "/vote";

            NotificationEventDto submitEvent = NotificationEventDto.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .type("MY_VOTE_SUBMITTED")
                    .targetType(TargetType.USER)
                    .targetId(String.valueOf(userId))
                    .args(submitArgs)
                    .redirectUrl(submitRedirectUrl)
                    .occurredAt(java.time.LocalDateTime.now())
                    .build();
            notificationProducer.send(submitEvent);
        } catch (Exception e) {
            log.error("MY_VOTE_SUBMITTED 알림 발행 실패: {}", e.getMessage());
        }

        return "투표가 완료되었습니다.";
    }

    @RateLimiter(name = "vote-db-protection", fallbackMethod = "rateLimitFallback")
    public String castVoteFallback(int voteId, int userId, int candidateNumber, String clientIp, Throwable t) {
        // 정상적인 중복 투표 방지 예외는 Redis 장애가 아니므로 그대로 던짐
        if (t.getMessage() != null && t.getMessage().contains("이미 투표에 참여하였습니다")) {
            throw new RuntimeException("이미 투표에 참여하였습니다.");
        }
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

    // RateLimiter Fallback — castVoteFallback이 Throwable을 하나 가지므로 Resilience4j가 Throwable 2개짜리 시그니처를 탐색
    public String rateLimitFallback(int voteId, int userId, int candidateNumber, String clientIp, Throwable t1, Throwable t2) {
        Throwable cause = t2 != null ? t2 : t1;
        if (cause instanceof RequestNotPermitted) {
            log.error("DB 보호를 위해 투표 요청 거절: userId={}", userId);
            throw new RuntimeException("현재 투표량이 많아 잠시 후 다시 시도해주세요.");
        }
        throw new RuntimeException(cause);
    }

    // 단일 Throwable 시그니처도 유지 (다른 경로에서 직접 호출될 경우 대비)
    public String rateLimitFallback(int voteId, int userId, int candidateNumber, String clientIp, Throwable t) {
        if (t instanceof RequestNotPermitted) {
            log.error("DB 보호를 위해 투표 요청 거절: userId={}", userId);
            throw new RuntimeException("현재 투표량이 많아 잠시 후 다시 시도해주세요.");
        }
        throw new RuntimeException(t);
    }

    private void saveToOutbox(int voteId, int userId, int candidateNumber, String redisKey) {
        String uuid = UUID.randomUUID().toString();
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("uuid", uuid);
        payloadMap.put("voteId", voteId);
        payloadMap.put("userId", userId);
        payloadMap.put("candidateNumber", candidateNumber);

        try {
            String payload = objectMapper.writeValueAsString(payloadMap);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(String.valueOf(voteId))
                    .aggregateType("VOTE")
                    .eventType("VOTE_CAST")
                    .payload(payload)
                    .build();
            outboxRepository.save(outboxEvent);
            log.info("투표 생성 이벤트 Outbox 저장 완료: voteId={}, userId={}", voteId, userId);
        } catch (JsonProcessingException e) {
            log.error("Outbox 페이로드 직렬화 실패: {}", e.getMessage());
            throw new RuntimeException("투표 처리 중 오류가 발생했습니다.");
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
        Optional<VoteRecord> recordOpt = voteRecordRepository.findByVoteIdAndUserId(voteId, userId);

        String redisVoteKey = "vote:" + voteId + ":user:" + userId;
        boolean redisKeyExists = Boolean.TRUE.equals(redisTemplate.hasKey(redisVoteKey));

        if (recordOpt.isEmpty() && !redisKeyExists) {
            throw new RuntimeException("투표 이력이 없습니다.");
        }

        VoteInfo voteInfo = voteReader.getVoteInfo(voteId);
        if (LocalDateTime.now().isAfter(voteInfo.getEndDate())) {
            throw new RuntimeException("이미 종료된 투표는 취소할 수 없습니다.");
        }

        // Kafka consumer가 아직 vote_record를 쓰지 않은 레이스 컨디션 처리
        if (recordOpt.isEmpty()) {
            String idempotencyKey = "processed:vote:" + voteId + ":user:" + userId;
            redisTemplate.opsForValue().set(idempotencyKey, "cancelled", Duration.ofMinutes(10));
            redisTemplate.delete(redisVoteKey);
            log.info("투표 취소 처리 (consumer 미처리 상태): voteId={}, userId={}", voteId, userId);
            return;
        }

        VoteRecord record = recordOpt.get();

        // Redis key 즉시 삭제 (hasVoted 체크 즉시 반영)
        redisTemplate.delete(redisVoteKey);

        // DB 차감: 기록 삭제, 후보자 득표수 감소, 투표 전체 참여자수 감소
        voteRecordRepository.delete(record);
        voteRecordRepository.flush();

        candidateRepository.decrementVoteCount(record.getCandidateId());
        candidateRepository.flush();

        com.bit.idol.voteservice.entity.Vote voteEntity = voteRepository.findById(voteId)
                .orElseThrow(() -> new RuntimeException("투표를 찾을 수 없습니다."));
        if (voteEntity.getTotalVotes() > 0) {
            voteEntity.setTotalVotes(voteEntity.getTotalVotes() - 1);
            voteRepository.saveAndFlush(voteEntity);
        }

        // Outbox 패턴 적용: Kafka/Redis 작업을 하나의 트랜잭션으로 묶어 Outbox 테이블에 저장
        com.bit.idol.voteservice.entity.Candidate candidate = candidateRepository.findById(record.getCandidateId())
                .orElseThrow(() -> new RuntimeException("후보자를 찾을 수 없습니다."));

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("voteId", voteId);
        payloadMap.put("userId", userId);
        payloadMap.put("candidateNumber", candidate.getNumber());
        payloadMap.put("redisKey", "vote:" + voteId + ":user:" + userId);
        payloadMap.put("processedKey", "processed:vote:" + voteId + ":user:" + userId);

        try {
            String payload = objectMapper.writeValueAsString(payloadMap);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(String.valueOf(voteId))
                    .aggregateType("VOTE")
                    .eventType("VOTE_CANCELLED")
                    .payload(payload)
                    .build();
            outboxRepository.save(outboxEvent);
            log.info("투표 취소 이벤트 Outbox 저장 완료: voteId={}, userId={}", voteId, userId);
        } catch (JsonProcessingException e) {
            log.error("Outbox 페이로드 직렬화 실패: {}", e.getMessage());
            throw new RuntimeException("투표 취소 처리 중 오류가 발생했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<MyVoteRecordDto> getMyVoteRecords(int userId, Long groupId) {
        return voteRecordRepository.findMyVoteRecords(userId, groupId);
    }
}
