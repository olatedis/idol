package com.bit.docker.subscriptionservice.config;

import com.bit.docker.subscriptionservice.dto.PaymentEvent;
import com.bit.docker.subscriptionservice.entity.Subscription;
import com.bit.docker.subscriptionservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedConsumer {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * 결제 완료 이벤트를 처리합니다.
     * SubscriptionService의 consume() 메서드와 중복되지 않도록 주의
     * 이 메서드는 삭제되거나 SubscriptionService로 통합되어야 합니다.
     * 
     * @deprecated SubscriptionService.consume() 메서드 사용 권장
     * 현재는 backup 용도로 유지 중
     */
    @Deprecated
    @KafkaListener(
            topics = "payment.completed",
            groupId = "subscription-service-backup"  // 별도 consumer group 사용
    )
    public void handle(String message) {
        try {
            log.debug("백업 결제 완료 이벤트 처리: message={}", message);
            PaymentEvent event = PaymentEvent.fromJson(message);

            if (!"SUBSCRIPTION".equals(event.getDomain())) {
                log.debug("구독이 아닌 결제 이벤트 무시: domain={}", event.getDomain());
                return;
            }

            Subscription subscription = subscriptionRepository
                    .findById(event.getTargetId())
                    .orElseThrow(() -> {
                        log.error("구독 정보 없음: subscriptionId={}", event.getTargetId());
                        return new IllegalArgumentException("구독 정보가 없습니다.");
                    });

            // 이미 활성화된 경우 중복 처리 방지
            if (subscription.isActive()) {
                log.warn("이미 활성화된 구독: subscriptionId={}, userId={}", 
                        subscription.getId(), subscription.getUserId());
                return;
            }

            subscription.activate();
            log.info("백업 처리로 구독 활성화: subscriptionId={}, userId={}, idolId={}", 
                    subscription.getId(), subscription.getUserId(), subscription.getIdolId());

        } catch (Exception e) {
            log.error("결제 완료 이벤트 처리 실패: error={}", e.getMessage(), e);
        }
    }
}
