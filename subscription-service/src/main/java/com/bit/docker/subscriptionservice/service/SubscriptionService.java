package com.bit.docker.subscriptionservice.service;

import com.bit.docker.subscriptionservice.dto.SubscriptionCancelRequest;
import com.bit.docker.subscriptionservice.dto.SubscriptionCreateRequest;
import com.bit.docker.subscriptionservice.dto.SubscriptionDto;
import com.bit.docker.subscriptionservice.dto.SubscriptionEvent;
import com.bit.docker.subscriptionservice.entity.Subscription;
import com.bit.docker.subscriptionservice.entity.SubscriptionStatus;
import com.bit.docker.subscriptionservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final StringRedisTemplate redisTemplate;
    private final SubscriptionEventProducer eventProducer;

    private static final String KEY_PREFIX = "sub:";

    /* =========================
       구독 생성
     ========================= */
    @Transactional
    public SubscriptionDto subscribe(int userId, SubscriptionCreateRequest request) {

        String redisKey = buildKey(userId, request.getIdolId());

        // 1. Redis 먼저 확인
        String cachedStatus = redisTemplate.opsForValue().get(redisKey);
        if (SubscriptionStatus.ACTIVE.name().equals(cachedStatus)) {
            throw new RuntimeException("이미 구독 중인 아이돌입니다.");
        }

        // 2. DB 확인
        subscriptionRepository.findByUserIdAndIdolId(userId, request.getIdolId())
                .ifPresent(sub -> {
                    if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
                        throw new RuntimeException("이미 구독 중인 아이돌입니다.");
                    }
                });

        LocalDateTime now = LocalDateTime.now();

        Subscription subscription = Subscription.builder()
                .userId(userId)
                .idolId(request.getIdolId())
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(now)
                .autoRenew(request.isAutoRenew())
                .build();

        subscriptionRepository.save(subscription);

        // 3. Redis 캐시 저장
        redisTemplate.opsForValue().set(redisKey, SubscriptionStatus.ACTIVE.name());

        // kafka 이벤트 생성
        eventProducer.publish(
                "subscription.created",
                SubscriptionEvent.builder()
                        .eventType("CREATED")
                        .userId(userId)
                        .idolId(request.getIdolId())
                        .occurredAt(LocalDateTime.now())
                        .build()
        );

        log.info("구독 생성 완료: userId={}, idolId={}", userId, request.getIdolId());

        return SubscriptionDto.fromEntity(subscription);
    }

    /* =========================
       구독 해지
     ========================= */
    @Transactional
    public void cancel(int userId, SubscriptionCancelRequest request) {

        Subscription subscription = subscriptionRepository
                .findByUserIdAndIdolId(userId, request.getIdolId())
                .orElseThrow(() -> new RuntimeException("구독 정보가 없습니다."));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new RuntimeException("활성 구독 상태가 아닙니다.");
        }

        subscription.cancel();

        // Redis 제거
        redisTemplate.delete(buildKey(userId, request.getIdolId()));

        // kafka 이벤트 생성
        eventProducer.publish(
                "subscription.canceled",
                SubscriptionEvent.builder()
                        .eventType("CANCELED")
                        .userId(userId)
                        .idolId(request.getIdolId())
                        .occurredAt(LocalDateTime.now())
                        .build()
        );

        log.info("구독 해지 완료: userId={}, idolId={}", userId, request.getIdolId());
    }

    /* =========================
       내 구독 목록 조회
     ========================= */
    public List<SubscriptionDto> getMySubscriptions(int userId) {
        return subscriptionRepository
                .findAllByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .stream()
                .map(SubscriptionDto::fromEntity)
                .toList();
    }

    /* =========================
       구독 여부 체크 (채팅 서비스용)
     ========================= */
    public boolean isSubscribed(int userId, Long idolId) {

        String redisKey = buildKey(userId, idolId);

        // Redis 우선
        String cachedStatus = redisTemplate.opsForValue().get(redisKey);
        if (cachedStatus != null) {
            return SubscriptionStatus.ACTIVE.name().equals(cachedStatus);
        }

        // DB fallback
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

    private String buildKey(int userId, Long idolId) {
        return KEY_PREFIX + userId + ":" + idolId;
    }
}

