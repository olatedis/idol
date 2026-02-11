package com.bit.paymentservice.service;

import com.bit.paymentservice.domain.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.topic.payment-completed}")
    private String topic;

    public void publish(PaymentEvent event) {
        kafkaTemplate.send(topic, event.toJson());
    }
}
