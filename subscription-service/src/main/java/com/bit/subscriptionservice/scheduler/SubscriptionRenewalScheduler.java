package com.bit.subscriptionservice.scheduler;

import com.bit.subscriptionservice.client.TossBillingKeyClient;
import com.bit.subscriptionservice.dto.PaymentEvent;
import com.bit.subscriptionservice.entity.Subscription;
import com.bit.subscriptionservice.entity.SubscriptionStatus;
import com.bit.subscriptionservice.repository.SubscriptionRepository;
import com.bit.subscriptionservice.service.BillingKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionRenewalScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final BillingKeyService billingKeyService;
    private final TossBillingKeyClient tossBillingKeyClient;

    /**
     * 만료된 구독 중 자동갱신이 활성화된 구독을 자동으로 갱신합니다.
     * 매일 자정에 실행
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void renewAutoSubscriptions() {
        log.info("구독 자동갱신 스케줄러 시작");

        try {
            // nextRenewalAt이 현재 시간 이전이고, 활성화 상태인 자동갱신 구독 조회
            List<Subscription> subscriptionsToRenew = subscriptionRepository.findAll()
                    .stream()
                    .filter(sub -> sub.isAutoRenew() &&
                            sub.getStatus() == SubscriptionStatus.ACTIVE &&
                            sub.getNextRenewalAt() != null &&
                            sub.getNextRenewalAt().isBefore(LocalDateTime.now()))
                    .toList();

            log.info("갱신 대상 구독 개수: {}", subscriptionsToRenew.size());

            for (Subscription subscription : subscriptionsToRenew) {
                try {
                    renewSubscription(subscription);
                } catch (Exception e) {
                    log.error("구독 갱신 실패: subscriptionId={}, userId={}, error={}", 
                            subscription.getId(), subscription.getUserId(), e.getMessage(), e);
                }
            }

            log.info("구독 자동갱신 스케줄러 완료");
        } catch (Exception e) {
            log.error("구독 자동갱신 스케줄러 실패: error={}", e.getMessage(), e);
        }
    }

    /**
     * 개별 구독을 갱신합니다.
     * 빌링키가 있으면 자동결제 처리, 없으면 결제 요청 이벤트 발행
     */
    @Transactional
    public void renewSubscription(Subscription subscription) {
        log.info("구독 갱신 처리: subscriptionId={}, userId={}, idolId={}", 
                subscription.getId(), subscription.getUserId(), subscription.getIdolId());

        subscription.renew();
        subscriptionRepository.save(subscription);

        // 빌링키가 있으면 자동결제 처리
        if (billingKeyService.hasBillingKey(subscription.getUserId(), subscription.getIdolId())) {
            try {
                processBillingKeyPayment(subscription);
            } catch (Exception e) {
                log.error("빌링키 결제 실패, 대체 결제 요청 이벤트 발행: subscriptionId={}, error={}", 
                        subscription.getId(), e.getMessage(), e);
                publishPaymentEvent(subscription);
            }
        } else {
            // 빌링키가 없으면 기존 결제 요청 이벤트 발행
            publishPaymentEvent(subscription);
        }
    }

    /**
     * 빌링키를 사용한 자동결제 처리
     */
    private void processBillingKeyPayment(Subscription subscription) {
        log.info("빌링키 자동결제 처리: subscriptionId={}, plan={}", 
                subscription.getId(), subscription.getPlan());

        String orderId = "SUB-" + subscription.getId() + "-" + System.currentTimeMillis();
        String orderName = subscription.getPlan().name() + " 정기구독 자동결제";
        int amount = subscription.getPlan().getAmount();

        try {
            // Toss 빌링키 결제 API 호출
            String paymentResult = tossBillingKeyClient.processBillingPayment(
                    subscription.getUserId(),
                    subscription.getIdolId(),
                    amount,
                    orderId,
                    orderName
            );

            log.info("빌링키 자동결제 성공: subscriptionId={}, orderId={}, amount={}", 
                    subscription.getId(), orderId, amount);

            // 결제 성공 이벤트 발행 (선택사항)
            PaymentEvent event = new PaymentEvent(
                    subscription.getUserId(),
                    null,
                    "SUBSCRIPTION_RENEWAL_BILLING_KEY",
                    subscription.getId(),
                    amount
            );
            kafkaTemplate.send("payment.completed", event.toJson());

        } catch (Exception e) {
            log.error("빌링키 자동결제 실패: subscriptionId={}, orderId={}, amount={}, error={}", 
                    subscription.getId(), orderId, amount, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 결제 요청 이벤트 발행
     */
    private void publishPaymentEvent(Subscription subscription) {
        PaymentEvent event = new PaymentEvent(
                subscription.getUserId(),
                null,
                "SUBSCRIPTION_RENEWAL",
                subscription.getId(),
                subscription.getPlan().getAmount()
        );

        kafkaTemplate.send("payment.requested", event.toJson());
        log.info("구독 갱신 결제 요청 발행: subscriptionId={}, amount={}", 
                subscription.getId(), subscription.getPlan().getAmount());
    }

    /**
     * 만료된 구독을 정리합니다.
     * 매일 오전 1시에 실행
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expireSubscriptions() {
        log.info("구독 만료 처리 스케줄러 시작");

        try {
            List<Subscription> expiredSubscriptions = subscriptionRepository.findAll()
                    .stream()
                    .filter(sub -> sub.getStatus() == SubscriptionStatus.ACTIVE &&
                            sub.getNextRenewalAt() != null &&
                            sub.getNextRenewalAt().isBefore(LocalDateTime.now().minusDays(7)) &&
                            !sub.isAutoRenew())
                    .toList();

            log.info("만료 대상 구독 개수: {}", expiredSubscriptions.size());

            for (Subscription subscription : expiredSubscriptions) {
                subscription.expire();
                subscriptionRepository.save(subscription);
                log.info("구독 만료 처리: subscriptionId={}, userId={}", 
                        subscription.getId(), subscription.getUserId());
            }

            log.info("구독 만료 처리 스케줄러 완료");
        } catch (Exception e) {
            log.error("구독 만료 처리 스케줄러 실패: error={}", e.getMessage(), e);
        }
    }
}
