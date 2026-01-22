package com.bit.docker.paymentservice.application;

import com.bit.docker.paymentservice.domain.dto.PaymentEvent;
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

    public void publish(String topic, PaymentEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, payload);

            log.info("Kafka 발행 성공: topic={}, orderId={}",
                    topic, event.getOrderId());
        } catch (JsonProcessingException e) {
            log.error("PaymentEvent JSON 직렬화 실패", e);
            throw new RuntimeException("Kafka payload 생성 실패");
        }
    }
}

