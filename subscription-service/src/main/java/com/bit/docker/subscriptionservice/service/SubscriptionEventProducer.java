package com.bit.docker.subscriptionservice.service;

import com.bit.docker.subscriptionservice.dto.SubscriptionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionEventProducer {

    private final KafkaTemplate<String, SubscriptionEvent> kafkaTemplate;

    public void publish(String topic, SubscriptionEvent event) {
        kafkaTemplate.send(topic, event);
        log.info("Kafka 이벤트 발행: topic={}, userId={}, idolId={}",
                topic, event.getUserId(), event.getIdolId());
    }
}

