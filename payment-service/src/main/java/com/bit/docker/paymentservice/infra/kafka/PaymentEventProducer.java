package com.bit.docker.paymentservice.infra.kafka;

import com.bit.docker.paymentservice.domain.entity.Payment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentCompleted(Payment payment) {
        kafkaTemplate.send("payment-completed", payment.getTargetId());
    }

    public void publishPaymentFailed(int targetId) {
        kafkaTemplate.send("payment-failed", targetId);
    }
}
