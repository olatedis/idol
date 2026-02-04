package com.bit.paymentservice.infra.kafka;

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
        kafkaTemplate.send("payment.completed", String.valueOf(payment.getTargetId()));
    }

    public void publishPaymentFailed(int targetId) {
        kafkaTemplate.send("payment-failed", String.valueOf(targetId));
    }
}
