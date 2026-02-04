// subscription-service
// 파일: com/bit/docker/subscriptionservice/scheduler/SubscriptionExpireScheduler.java
package com.bit.docker.subscriptionservice.scheduler;

import com.bit.docker.subscriptionservice.dto.SubscriptionEvent;
import com.bit.docker.subscriptionservice.entity.GroupSubscription;
import com.bit.docker.subscriptionservice.entity.Subscription;
import com.bit.docker.subscriptionservice.entity.SubscriptionStatus;
import com.bit.docker.subscriptionservice.repository.GroupSubscriptionRepository;
import com.bit.docker.subscriptionservice.repository.SubscriptionRepository;
import com.bit.docker.subscriptionservice.service.SubscriptionEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpireScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final GroupSubscriptionRepository groupSubscriptionRepository;
    private final StringRedisTemplate redisTemplate;
    private final SubscriptionEventProducer eventProducer;

    private static final String KEY_PREFIX_IDOL = "sub:";
    private static final String KEY_PREFIX_GROUP = "gsub:";

    @Scheduled(cron = "0 0 * * * *") // 매 시간
    @Transactional
    public void expireSubscriptions() {

        // 개인(아이돌) 구독 만료 처리
        List<Subscription> expiredIdolTargets =
                subscriptionRepository.findAllByStatusAndExpiredAtBefore(
                        SubscriptionStatus.ACTIVE,
                        LocalDateTime.now()
                );

        for (Subscription sub : expiredIdolTargets) {
            sub.expire();

            redisTemplate.delete(
                    KEY_PREFIX_IDOL + sub.getUserId() + ":" + sub.getIdolId()
            );

            String uuid = UUID.randomUUID().toString();
            Map<String, String> args = new HashMap<>();
            args.put("userId", String.valueOf(sub.getUserId()));
            args.put("idolId", String.valueOf(sub.getIdolId()));

            eventProducer.publish(
                    SubscriptionEvent.builder()
                            .eventId(uuid)
                            .type("IDOL_SUB_END")
                            .targetType(SubscriptionEvent.TargetType.USER)
                            .targetId(String.valueOf(sub.getUserId()))
                            .args(args)
                            .redirectUrl("/subscription") //TODO: 나중에 라우팅 제대로 맞추기.
                            .occurredAt(LocalDateTime.now())
                            .build()
            );
        }

//        // 그룹 구독 만료 처리
//        List<GroupSubscription> expiredGroupTargets =
//                groupSubscriptionRepository.findAllByStatusAndExpiredAtBefore(
//                        SubscriptionStatus.ACTIVE,
//                        LocalDateTime.now()
//                );
//
//        for (GroupSubscription gs : expiredGroupTargets) {
//            gs.expire();
//
//            redisTemplate.delete(
//                    KEY_PREFIX_GROUP + gs.getUserId() + ":" + gs.getGroupId()
//            );
//
//            eventProducer.publish(
//                    "group-subscription.expired",
//                    SubscriptionEvent.builder()
//                            .eventType("EXPIRED")
//                            .targetType(SubscriptionEvent.TargetType.GROUP)
//                            .userId(gs.getUserId())
//                            .idolId(0)
//                            .groupId(gs.getGroupId())
//                            .occurredAt(LocalDateTime.now())
//                            .build()
//            );
//        }

        log.info("구독 만료 처리 완료: idolCount={}", expiredIdolTargets.size());
    }
}
