package com.bit.docker.paymentservice.infra.kafka;

import com.bit.docker.paymentservice.application.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReservationEventConsumer {

    private final PaymentService paymentService;

    public ReservationEventConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "reservation-created")
    public void consume(String orderId, Long userId) {
        paymentService.createPayment(orderId, userId);
    }
}
