package com.bit.docker.subscriptionservice.scheduler;

import com.bit.docker.subscriptionservice.dto.SubscriptionEvent;
import com.bit.docker.subscriptionservice.entity.Subscription;
import com.bit.docker.subscriptionservice.entity.SubscriptionStatus;
import com.bit.docker.subscriptionservice.repository.SubscriptionRepository;
import com.bit.docker.subscriptionservice.service.SubscriptionEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpireScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final StringRedisTemplate redisTemplate;
    private final SubscriptionEventProducer eventProducer;

    private static final String KEY_PREFIX = "sub:";

    @Scheduled(cron = "0 0 * * * *") // 매 시간
    @Transactional
    public void expireSubscriptions() {

        List<Subscription> expiredTargets =
                subscriptionRepository.findAllByStatusAndExpiredAtBefore(
                        SubscriptionStatus.ACTIVE,
                        LocalDateTime.now()
                );

        for (Subscription sub : expiredTargets) {
            sub.expire();

            redisTemplate.delete(
                    KEY_PREFIX + sub.getUserId() + ":" + sub.getIdolId()
            );

            eventProducer.publish(
                    "subscription.expired",
                    SubscriptionEvent.builder()
                            .eventType("EXPIRED")
                            .userId(sub.getUserId())
                            .idolId(sub.getIdolId())
                            .occurredAt(LocalDateTime.now())
                            .build()
            );
        }

        log.info("구독 만료 처리 완료: count={}", expiredTargets.size());
    }
}

