package com.bit.docker.paymentservice.infra.kafka;

import com.bit.docker.paymentservice.domain.dto.PaymentCreateRequest;
import com.bit.docker.paymentservice.domain.dto.PaymentEvent;
import com.bit.docker.paymentservice.infra.persistence.PaymentRepository;
import com.bit.docker.paymentservice.service.PaymentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Slf4j
@AllArgsConstructor
public class ReservationEventConsumer {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @KafkaListener(
            topics = "payment.requested",
            groupId = "payment-service"
    )
    public void consume(String message) {

        PaymentEvent event =
                PaymentEvent.fromJson(message);

        PaymentCreateRequest payment = new PaymentCreateRequest(
                event.getUserId(),
                event.getAmount(),
                event.getDomain(),
                event.getTargetId()
        );

        paymentService.createPayment(payment);

    }
}
