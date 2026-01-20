package com.bit.docker.paymentservice.application;

import com.bit.docker.paymentservice.domain.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducerService {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public void publish(String topic, PaymentEvent event) {
        kafkaTemplate.send(topic, event);
        log.info("발행: topic={}, orderId={}", topic, event.getOrderId());
    }
}

