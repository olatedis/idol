package com.bit.docker.subscriptionservice.service;

import com.bit.docker.subscriptionservice.dto.*;
import com.bit.docker.subscriptionservice.entity.GroupSubscription;
import com.bit.docker.subscriptionservice.entity.Subscription;
import com.bit.docker.subscriptionservice.entity.SubscriptionStatus;
import com.bit.docker.subscriptionservice.repository.GroupSubscriptionRepository;
import com.bit.docker.subscriptionservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final GroupSubscriptionRepository groupSubscriptionRepository;
    private final StringRedisTemplate redisTemplate;
    private final SubscriptionEventProducer eventProducer;
    private final KafkaTemplate<String, String> kafkaTemplate;


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

        subscriptionRepository.findByUserIdAndIdolId(userId, request.getIdolId())
                .ifPresent(sub -> {
                    if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
                        throw new RuntimeException("이미 구독 중인 아이돌입니다.");
                    }
                });

        Subscription subscription = Subscription.builder()
                .userId(userId)
                .idolId(request.getIdolId())
                .status(SubscriptionStatus.PENDING)
                .plan(request.getPlan())
                .autoRenew(request.isAutoRenew())
                .build();

        subscriptionRepository.save(subscription);

        redisTemplate.opsForValue().set(redisKey, SubscriptionStatus.PENDING.name());

        PaymentEvent event = new PaymentEvent(
                userId,
                null,
                "SUBSCRIPTION",
                subscription.getId(),
                request.getPlan().getAmount()
        );

        kafkaTemplate.send("payment.requested", event.toJson());

        log.info("개인(아이돌) 구독 준비 완료: userId={}, idolId={}, plan={}, amount={}", 
                userId, request.getIdolId(), request.getPlan(), request.getPlan().getAmount());
        return SubscriptionDto.fromEntity(subscription);
    }

    @KafkaListener(
            topics = "payment.completed",
            groupId = "subscription-service"
    )
    public void consume(String message) {

        PaymentEvent event =
                PaymentEvent.fromJson(message);

        if (!"SUBSCRIPTION".equals(event.getDomain())) {
            return;
        }

        Subscription subscription =
                subscriptionRepository
                        .findByUserIdAndIdolIdAndStatus(
                                event.getUserId(),
                                event.getTargetId(),
                                SubscriptionStatus.PENDING
                        )
                        .orElseThrow();

        subscription.activate();

        String uuid = UUID.randomUUID().toString();
        Map<String, String> args = new HashMap<>();
        args.put("userId", String.valueOf(subscription.getUserId()));
        args.put("idolId", String.valueOf(subscription.getIdolId()));
        args.put("startAt", subscription.getStartedAt().toString());
        args.put("expireAt", subscription.getExpiredAt().toString());
        eventProducer.publish(
                "IDOL_SUB_STARTED",
                SubscriptionEvent.builder()
                        .eventId(uuid)
                        .targetType(SubscriptionEvent.TargetType.USER)
                        .targetId(String.valueOf(subscription.getUserId()))
                        .args(args)
                        .occurredAt(LocalDateTime.now())
                        .build()
        );

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
        eventProducer.publish(
                "IDOL_SUB_END",
                SubscriptionEvent.builder()
                        .eventId(uuid)
                        .targetType(SubscriptionEvent.TargetType.USER)
                        .targetId(String.valueOf(subscription.getUserId()))
                        .args(args)
                        .occurredAt(LocalDateTime.now())
                        .build()
        );

        log.info("개인(아이돌) 구독 해지 완료: userId={}, idolId={}", userId, request.getIdolId());
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
                SubscriptionStatus.ACTIVE
        );

        if (active) {
            redisTemplate.opsForValue().set(redisKey, SubscriptionStatus.ACTIVE.name());
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
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(now)
                .autoRenew(request.isAutoRenew())
                .build();

        groupSubscriptionRepository.save(gs);

        redisTemplate.opsForValue().set(redisKey, SubscriptionStatus.ACTIVE.name());

        // 그룹 구독 이벤트
        String uuid = UUID.randomUUID().toString();
        Map<String, String> args = new HashMap<>();
        args.put("groupId", String.valueOf(gs.getGroupId()));
        eventProducer.publish(
                "GROUP_SUB_STARTED",
                SubscriptionEvent.builder()
                        .eventId(uuid)
                        .targetType(SubscriptionEvent.TargetType.USER)
                        .targetId(String.valueOf(gs.getUserId()))
                        .args(args)
                        .occurredAt(LocalDateTime.now())
                        .build()
        );

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
        eventProducer.publish(
                "GROUP_SUB_END",
                SubscriptionEvent.builder()
                        .eventId(uuid)
                        .targetType(SubscriptionEvent.TargetType.USER)
                        .targetId(String.valueOf(gs.getUserId()))
                        .args(args)
                        .occurredAt(LocalDateTime.now())
                        .build()
        );

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
                SubscriptionStatus.ACTIVE
        );

        if (active) {
            redisTemplate.opsForValue().set(redisKey, SubscriptionStatus.ACTIVE.name());
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
                SubscriptionStatus.ACTIVE
        );
    }

    public boolean isActiveGroupSubscriber(int userId, int groupId) {
        return groupSubscriptionRepository.existsByUserIdAndGroupIdAndStatus(
                userId,
                groupId,
                SubscriptionStatus.ACTIVE
        );
    }


}
