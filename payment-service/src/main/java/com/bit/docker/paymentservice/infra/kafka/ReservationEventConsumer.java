package com.bit.docker.paymentservice.infra.kafka;

import com.bit.docker.paymentservice.application.PaymentCommandService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReservationEventConsumer {

    private final PaymentCommandService paymentCommandService;

    public ReservationEventConsumer(PaymentCommandService paymentCommandService) {
        this.paymentCommandService = paymentCommandService;
    }

    @KafkaListener(topics = "reservation-created")
    public void consume(Long reservationId) {
        paymentCommandService.createPayment(reservationId);
    }
}
