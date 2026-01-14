package com.bit.docker.paymentservice.infra.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentCompleted(Long reservationId) {
        kafkaTemplate.send("payment-completed", reservationId);
    }

    public void publishPaymentFailed(Long reservationId) {
        kafkaTemplate.send("payment-failed", reservationId);
    }
}
