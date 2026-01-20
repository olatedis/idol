package com.bit.docker.paymentservice.infra.kafka;

import com.bit.docker.paymentservice.application.PaymentService;
import com.bit.docker.paymentservice.domain.dto.PaymentCreateRequest;
import com.bit.docker.paymentservice.domain.dto.TossConfirmRequest;
import com.bit.docker.paymentservice.domain.enumtype.PaymentDomain;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReservationEventConsumer {

    private final PaymentService paymentService;

    public ReservationEventConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "payment-created")
    public void consume(int targetId, int amount, PaymentDomain domain, int userId) {
        PaymentCreateRequest request = new PaymentCreateRequest( targetId, domain, amount);
        paymentService.createPayment(request,  userId);
    }
}
