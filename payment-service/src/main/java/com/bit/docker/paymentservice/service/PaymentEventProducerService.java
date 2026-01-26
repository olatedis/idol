package com.bit.docker.paymentservice.service;

import com.bit.docker.paymentservice.domain.dto.PaymentEvent;
import com.bit.docker.paymentservice.domain.entity.Payment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishCompleted(Payment payment) {
        PaymentEvent event =
                new PaymentEvent(
                        payment.getUserId(),
                        payment.getOrderId(),
                        payment.getDomain(),
                        payment.getTargetId(),
                        payment.getAmount()
                );

        kafkaTemplate.send("payment.completed", event.toJson());
    }
}

