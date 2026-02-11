package com.bit.paymentservice.listener;

import com.bit.paymentservice.domain.event.PaymentCompletedEvent;
import com.bit.paymentservice.service.PaymentEventProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final PaymentEventProducerService paymentEventProducerService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent eventWrapper) {
        log.info("결제 완료 이벤트 발행 (After Commit): orderId={}", eventWrapper.event().getOrderId());
        try {
            paymentEventProducerService.publish(eventWrapper.event());
        } catch (Exception e) {
            log.error("Kafka 이벤트 발행 실패: orderId={}, error={}", eventWrapper.event().getOrderId(), e.getMessage());
        }
    }
}
