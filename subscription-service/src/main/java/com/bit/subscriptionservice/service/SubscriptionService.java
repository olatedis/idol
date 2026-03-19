package com.bit.subscriptionservice.service;

import com.bit.subscriptionservice.client.UserServiceClient;
import com.bit.subscriptionservice.dto.*;
import com.bit.subscriptionservice.dto.event.PaymentRequestEvent;
import com.bit.subscriptionservice.dto.event.SubscriptionEventWrapper;
import com.bit.subscriptionservice.entity.GroupSubscription;
import com.bit.subscriptionservice.entity.Subscription;
import com.bit.subscriptionservice.entity.SubscriptionStatus;
import com.bit.subscriptionservice.repository.GroupSubscriptionRepository;
import com.bit.subscriptionservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final GroupSubscriptionRepository groupSubscriptionRepository;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher; // 추가됨
    private final UserServiceClient userServiceClient;

    private static final String KEY_PREFIX_IDOL = "sub:";
    private static final String KEY_PREFIX_GROUP = "gsub:";

    // 개인(아이돌) 구독 생성
    @Transactional
    public SubscriptionDto subscribe(int userId, SubscriptionCreateRequest request) {

        String redisKey = buildIdolKey(userId, request.getIdolId());

        String cachedStatus = redisTemplate.opsForValue().get(redisKey);
        if (SubscriptionStatus.ACTIVE.name().equals(cachedStatus)) {
            throw new RuntimeException("이미 구독 중인 아이돌입니다.");
        }

        Optional<Subscription> existingSub = subscriptionRepository.findByUserIdAndIdolId(userId, request.getIdolId());
        
        Subscription subscription;
        if (existingSub.isPresent()) {
            subscription = existingSub.get();
            if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                throw new RuntimeException("이미 구독 중인 아이돌입니다.");
            }
            // PENDING이나 EXPIRED 등인 경우 정보를 업데이트하여 재사용
            subscription.update(request.getPlan(), request.isAutoRenew());
        } else {
            // 신규 가입
            subscription = Subscription.builder()
                    .userId(userId)
                    .idolId(request.getIdolId())
                    .status(SubscriptionStatus.PENDING)
                    .startedAt(LocalDateTime.now())
                    .plan(request.getPlan())
                    .autoRenew(request.isAutoRenew())
                    .build();
            subscriptionRepository.save(subscription);
        }

        redisTemplate.opsForValue().set(redisKey, SubscriptionStatus.PENDING.name(), Duration.ofDays(1)); // PENDING은 짧게 1일

        PaymentEvent event = new PaymentEvent(
                userId,
                null,
                "SUBSCRIPTION",
                subscription.getId(),
                request.getPlan().getAmount(),
                0 // agencyId
        );

        // 이벤트 발행 (커밋 후 실행됨)
        eventPublisher.publishEvent(new PaymentRequestEvent(event));

        log.info("개인(아이돌) 구독 준비 완료: userId={}, idolId={}, plan={}, amount={}",
                userId, request.getIdolId(), request.getPlan(), request.getPlan().getAmount());
        return SubscriptionDto.fromEntity(subscription);
    }

    /**
     * 빌링키 발급 이후 PENDING 상태인 구독을 활성화한다.
     * (정기결제 흐름에서 사용)
     */
    @Transactional
    public void activatePendingSubscription(int userId, int idolId) {
        Subscription subscription = subscriptionRepository
                .findByUserIdAndIdolIdAndStatus(userId, idolId, SubscriptionStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("활성화할 구독 정보가 없습니다."));

        subscription.activate();
        // cache 상태를 ACTIVE로 갱신
        String redisKey = buildIdolKey(userId, idolId);
        redisTemplate.opsForValue().set(redisKey, SubscriptionStatus.ACTIVE.name(), Duration.ofDays(30));

        // publish same event as in Kafka listener
        String uuid = UUID.randomUUID().toString();
        Map<String, String> args = new HashMap<>();
        args.put("userId", String.valueOf(subscription.getUserId()));
        args.put("idolId", String.valueOf(subscription.getIdolId()));
        args.put("startAt", subscription.getStartedAt().toString());
        args.put("expiredAt", subscription.getExpiredAt().toString());

        SubscriptionEvent subEvent = SubscriptionEvent.builder()
                .eventId(uuid)
                .type("IDOL_SUB_STARTED")
                .targetType(SubscriptionEvent.TargetType.USER)
                .targetId(String.valueOf(subscription.getUserId()))
                .args(args)
                .redirectUrl("/mypage?tab=subscription")
                .occurredAt(LocalDateTime.now())
                .build();

        eventPublisher.publishEvent(new SubscriptionEventWrapper("IDOL_SUB_STARTED", subEvent));

        // [추가] 그룹 자동 구독 처리
        tryAutoSubscribeGroup(subscription.getUserId(), subscription.getIdolId());

        log.info("PENDING 구독 활성화 (billing): userId={}, idolId={}", userId, idolId);
    }

    @KafkaListener(topics = "payment.completed", groupId = "subscription-service")
    @Transactional
    public void consume(String message) {

        PaymentEvent event = PaymentEvent.fromJson(message);

        if (!"SUBSCRIPTION".equals(event.getDomain())) {
            return;
        }

        int subscriptionId = event.getTargetId();

        Subscription subscription = subscriptionRepository
                .findByIdAndUserIdAndStatus(
                        subscriptionId,
                        event.getUserId(),
                        SubscriptionStatus.PENDING)
                .orElseThrow();

        subscription.activate();
        // redis 캐시도 함께 갱신
        String redisKey = buildIdolKey(subscription.getUserId(), subscription.getIdolId());
        redisTemplate.opsForValue().set(redisKey, SubscriptionStatus.ACTIVE.name(), Duration.ofDays(30));

        String uuid = UUID.randomUUID().toString();
        Map<String, String> args = new HashMap<>();
        args.put("userId", String.valueOf(subscription.getUserId()));
        args.put("idolId", String.valueOf(subscription.getIdolId()));
        args.put("startAt", subscription.getStartedAt().toString());
        args.put("expiredAt", subscription.getExpiredAt().toString());

        SubscriptionEvent subEvent = SubscriptionEvent.builder()
                .eventId(uuid)
                .type("IDOL_SUB_STARTED")
                .targetType(SubscriptionEvent.TargetType.USER)
                .targetId(String.valueOf(subscription.getUserId()))
                .args(args)
                .redirectUrl("/mypage?tab=subscription")
                .occurredAt(LocalDateTime.now())
                .build();

        // 이벤트 발행 (커밋 후 실행됨)
        eventPublisher.publishEvent(new SubscriptionEventWrapper("IDOL_SUB_STARTED", subEvent));

        // [추가] 그룹 자동 구독 처리
        tryAutoSubscribeGroup(subscription.getUserId(), subscription.getIdolId());

        log.info("개인(아이돌) 구독 완료: userId={}, idolId={}", subscription.getUserId(), subscription.getIdolId());
    }

    // 개인(아이돌) 구독 해지
    @Transactional
    public void cancel(int userId, SubscriptionCancelRequest request) {

        Subscription subscription = subscriptionRepository
                .findByUserIdAndIdolId(userId, request.getIdolId())
                .orElseThrow(() -> new RuntimeException("구독 정보가 없습니다."));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new RuntimeException("활성 구독 상태가 아닙니다.");
        }

        subscription.cancel();

        redisTemplate.delete(buildIdolKey(userId, request.getIdolId()));

        // 개인 구독 해지 이벤트
        String uuid = UUID.randomUUID().toString();
        Map<String, String> args = new HashMap<>();
        args.put("userId", String.valueOf(subscription.getUserId()));
        args.put("idolId", String.valueOf(subscription.getIdolId()));

        SubscriptionEvent subEvent = SubscriptionEvent.builder()
                .eventId(uuid)
                .type("IDOL_SUB_END")
                .targetType(SubscriptionEvent.TargetType.USER)
                .targetId(String.valueOf(subscription.getUserId()))
                .args(args)
                .redirectUrl("/mypage?tab=subscription")
                .occurredAt(LocalDateTime.now())
                .build();

        // 이벤트 발행
        eventPublisher.publishEvent(new SubscriptionEventWrapper("IDOL_SUB_END", subEvent));

        log.info("개인(아이돌) 구독 해지 완료: userId={}, idolId={}", userId, request.getIdolId());
    }

    /**
     * 결제 실패 시 생성된 PENDING 상태 구독을 삭제한다.
     */
    @Transactional
    public void deletePending(int userId, int subscriptionId) {
        Subscription subscription = subscriptionRepository
                .findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("구독 정보가 없습니다."));

        if (subscription.getUserId() != userId) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        if (subscription.getStatus() != SubscriptionStatus.PENDING) {
            throw new IllegalArgumentException("삭제할 수 없는 상태의 구독입니다.");
        }

        subscriptionRepository.delete(subscription);
        log.info("Pending 구독 삭제: subscriptionId={}, userId={}", subscriptionId, userId);
    }

    // 내 개인(아이돌) 구독 목록 조회
    public List<SubscriptionDto> getMySubscriptions(int userId) {
        return subscriptionRepository
                .findAllByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .stream()
                .map(SubscriptionDto::fromEntity)
                .toList();
    }

    // 개인(아이돌) 구독 여부 체크(채팅 서비스용)
    public boolean isSubscribed(int userId, int idolId) {

        String redisKey = buildIdolKey(userId, idolId);

        String cachedStatus = redisTemplate.opsForValue().get(redisKey);
        if (cachedStatus != null) {
            return SubscriptionStatus.ACTIVE.name().equals(cachedStatus);
        }

        boolean active = subscriptionRepository.existsByUserIdAndIdolIdAndStatus(
                userId,
                idolId,
                SubscriptionStatus.ACTIVE);

        if (active) {
            redisTemplate.opsForValue().set(redisKey, SubscriptionStatus.ACTIVE.name(), Duration.ofDays(30));
        }

        return active;
    }

    // 그룹 구독 생성
    @Transactional
    public GroupSubscriptionDto subscribeGroup(int userId, GroupSubscriptionCreateRequest request) {

        String redisKey = buildGroupKey(userId, request.getGroupId());

        String cachedStatus = redisTemplate.opsForValue().get(redisKey);
        if (SubscriptionStatus.ACTIVE.name().equals(cachedStatus)) {
            throw new RuntimeException("이미 구독 중인 그룹입니다.");
        }

        groupSubscriptionRepository.findByUserIdAndGroupId(userId, request.getGroupId())
                .ifPresent(gs -> {
                    if (gs.getStatus() == SubscriptionStatus.ACTIVE) {
                        throw new RuntimeException("이미 구독 중인 그룹입니다.");
                    }
                });

        LocalDateTime now = LocalDateTime.now();

        GroupSubscription gs = GroupSubscription.builder()
                .userId(userId)
                .groupId(request.getGroupId())
                .groupName(request.getGroupName())
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(now)
                .autoRenew(request.isAutoRenew())
                .build();

        groupSubscriptionRepository.save(gs);

        redisTemplate.opsForValue().set(redisKey, SubscriptionStatus.ACTIVE.name(), Duration.ofDays(30));

        // 그룹 구독 이벤트
        String uuid = UUID.randomUUID().toString();
        Map<String, String> args = new HashMap<>();
        args.put("groupId", String.valueOf(gs.getGroupId()));

        SubscriptionEvent subEvent = SubscriptionEvent.builder()
                .eventId(uuid)
                .type("GROUP_SUB_STARTED")
                .targetType(SubscriptionEvent.TargetType.USER)
                .targetId(String.valueOf(gs.getUserId()))
                .args(args)
                .redirectUrl("/mypage?tab=subscription")
                .occurredAt(LocalDateTime.now())
                .build();

        // 이벤트 발행
        eventPublisher.publishEvent(new SubscriptionEventWrapper("GROUP_SUB_STARTED", subEvent));

        log.info("그룹 구독 생성 완료: userId={}, groupId={}", userId, request.getGroupId());
        return GroupSubscriptionDto.fromEntity(gs);
    }

    // 그룹 구독 해지
    @Transactional
    public void cancelGroup(int userId, GroupSubscriptionCancelRequest request) {

        GroupSubscription gs = groupSubscriptionRepository
                .findByUserIdAndGroupId(userId, request.getGroupId())
                .orElseThrow(() -> new RuntimeException("구독 정보가 없습니다."));

        if (gs.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new RuntimeException("활성 구독 상태가 아닙니다.");
        }

        gs.cancel();

        redisTemplate.delete(buildGroupKey(userId, request.getGroupId()));

        // 그룹 구독 해지 이벤트
        String uuid = UUID.randomUUID().toString();
        Map<String, String> args = new HashMap<>();
        args.put("userId", String.valueOf(gs.getUserId()));
        args.put("groupId", String.valueOf(gs.getGroupId()));

        SubscriptionEvent subEvent = SubscriptionEvent.builder()
                .eventId(uuid)
                .type("GROUP_SUB_END")
                .targetType(SubscriptionEvent.TargetType.USER)
                .targetId(String.valueOf(gs.getUserId()))
                .args(args)
                .redirectUrl("/mypage?tab=subscription")
                .occurredAt(LocalDateTime.now())
                .build();

        // 이벤트 발행
        eventPublisher.publishEvent(new SubscriptionEventWrapper("GROUP_SUB_END", subEvent));

        log.info("그룹 구독 해지 완료: userId={}, groupId={}", userId, request.getGroupId());
    }

    // 내 그룹 구독 목록 조회
    public List<GroupSubscriptionDto> getMyGroupSubscriptions(int userId) {
        return groupSubscriptionRepository
                .findAllByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .stream()
                .map(GroupSubscriptionDto::fromEntity)
                .toList();
    }

    // 그룹 구독 여부 체크(필요하면 사용)
    public boolean isGroupSubscribed(int userId, int groupId) {

        String redisKey = buildGroupKey(userId, groupId);

        String cachedStatus = redisTemplate.opsForValue().get(redisKey);
        if (cachedStatus != null) {
            return SubscriptionStatus.ACTIVE.name().equals(cachedStatus);
        }

        boolean active = groupSubscriptionRepository.existsByUserIdAndGroupIdAndStatus(
                userId,
                groupId,
                SubscriptionStatus.ACTIVE);

        if (active) {
            redisTemplate.opsForValue().set(redisKey, SubscriptionStatus.ACTIVE.name(), Duration.ofDays(30));
        }

        return active;
    }

    // fanout용: idolId(개인 아이돌) ACTIVE 구독자 userId 리스트
    public List<Integer> getActiveSubscriberUserIdsByIdolId(int idolId) {
        return subscriptionRepository.selectUserIdsByIdolIdAndStatus(idolId, SubscriptionStatus.ACTIVE);
    }

    // fanout용: groupId(그룹) ACTIVE 구독자 userId 리스트
    public List<Integer> getActiveSubscriberUserIdsByGroupId(int groupId) {
        return groupSubscriptionRepository.selectUserIdsByGroupIdAndStatus(groupId, SubscriptionStatus.ACTIVE);
    }

    private String buildIdolKey(int userId, int idolId) {
        return KEY_PREFIX_IDOL + userId + ":" + idolId;
    }

    private String buildGroupKey(int userId, int groupId) {
        return KEY_PREFIX_GROUP + userId + ":" + groupId;
    }

    public boolean isActiveIdolSubscriber(int userId, int idolId) {
        return subscriptionRepository.existsByUserIdAndIdolIdAndStatus(
                userId,
                idolId,
                SubscriptionStatus.ACTIVE);
    }

    public boolean isActiveGroupSubscriber(int userId, int groupId) {
        return groupSubscriptionRepository.existsByUserIdAndGroupIdAndStatus(
                userId,
                groupId,
                SubscriptionStatus.ACTIVE);
    }

    // 아이돌 구독자 수 조회
    public int getSubscriptionCount(int idolId) {
        return subscriptionRepository.countByIdolIdAndStatus(idolId, SubscriptionStatus.ACTIVE);
    }

    /**
     * [내부 로직] 아이돌 구독 시 해당 아이돌이 속한 그룹을 자동 구독한다.
     */
    private void tryAutoSubscribeGroup(int userId, int idolId) {
        try {
            IdolResponse idol = userServiceClient.getIdol(idolId);
            if (idol != null && idol.getGroupId() != null) {
                int groupId = idol.getGroupId();
                // 이미 해당 그룹을 구독 중인지 체크
                if (!isGroupSubscribed(userId, groupId)) {
                    log.info("그룹 자동 구독 실행: userId={}, groupId={}, groupName={}",
                            userId, groupId, idol.getGroupName());

                    GroupSubscriptionCreateRequest request = new GroupSubscriptionCreateRequest(
                            groupId,
                            idol.getGroupName(),
                            true // 자동 갱신 기본값
                    );
                    subscribeGroup(userId, request);
                }
            }
        } catch (Exception e) {
            log.warn("그룹 자동 구독 중 오류 발생 (무시하고 진행): userId={}, idolId={}, error={}",
                    userId, idolId, e.getMessage());
        }
    }

}
