package com.bit.paymentservice.infra.kafka;

import com.bit.paymentservice.domain.dto.PaymentEvent;
import com.bit.paymentservice.domain.entity.Payment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentCompleted(Payment payment) {
        PaymentEvent event = new PaymentEvent(
                payment.getUserId(),
                payment.getOrderId(),
                payment.getDomain(),
                payment.getTargetId(),
                payment.getAmount(),
                payment.getAgencyId(),
                payment.deserializeReservationIds(),
                null);

        kafkaTemplate.send("payment.completed", event.toJson());
    }

    public void publishPaymentFailed(int targetId) {
        kafkaTemplate.send("payment-failed", String.valueOf(targetId));
    }
}
