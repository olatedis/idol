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

    public void publish(PaymentEvent event) {

        kafkaTemplate.send("payment.completed", event.toJson());
    }
}

