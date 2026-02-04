package com.bit.paymentservice.service;

import com.bit.paymentservice.domain.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(PaymentEvent event) {

        kafkaTemplate.send("payment.completed", event.toJson());
    }
}

