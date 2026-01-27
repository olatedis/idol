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

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(String topic, SubscriptionEvent event) {
        kafkaTemplate.send(topic, event.toJson());
        log.info("Kafka 이벤트 발행: topic={}, eventType={}, targetType={}, userId={}, idolId={}, groupId={}",
                topic,
                event.getEventType(),
                event.getTargetType(),
                event.getUserId(),
                event.getIdolId(),
                event.getGroupId()
        );
    }
}

