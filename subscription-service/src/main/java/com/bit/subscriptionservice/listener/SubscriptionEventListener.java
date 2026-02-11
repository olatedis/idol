package com.bit.subscriptionservice.listener;

import com.bit.subscriptionservice.dto.event.PaymentRequestEvent;
import com.bit.subscriptionservice.dto.event.SubscriptionEventWrapper;
import com.bit.subscriptionservice.service.SubscriptionEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionEventListener {

    @Value("${spring.kafka.topic.payment-requested}")
    private String paymentRequestedTopic;

    private final SubscriptionEventProducer eventProducer;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // 구독/해지 알림 이벤트 (DB 커밋 후 실행)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSubscriptionEvent(SubscriptionEventWrapper wrapper) {
        log.info("Subscription Event 발행 (After Commit): type={}", wrapper.event().getType());
        try {
            eventProducer.publish(wrapper.topic(), wrapper.event());
        } catch (Exception e) {
            log.error("Kafka 이벤트 발행 실패: {}", e.getMessage());
        }
    }

    // 결제 요청 이벤트 (DB 커밋 후 실행)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentRequest(PaymentRequestEvent wrapper) {
        log.info("Payment Request 발행 (After Commit): userId={}", wrapper.event().getUserId());
        try {
            kafkaTemplate.send(paymentRequestedTopic, wrapper.event().toJson());
        } catch (Exception e) {
            log.error("Kafka 결제 요청 발행 실패: {}", e.getMessage());
        }
    }
}
