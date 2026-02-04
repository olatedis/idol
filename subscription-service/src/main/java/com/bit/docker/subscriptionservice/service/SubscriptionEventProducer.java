package com.bit.docker.subscriptionservice.service;

import com.bit.docker.subscriptionservice.dto.SubscriptionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * subscription-service 알림 발행 전용 Producer
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionEventProducer {

    private static final String NOTIFY_TOPIC = "notify-request-topic";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(SubscriptionEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(NOTIFY_TOPIC, json);

            log.info("구독 알림 발행: topic={}, type={}, targetType={}, targetId={}",
                    NOTIFY_TOPIC,
                    event.getType(),
                    event.getTargetType(),
                    event.getTargetId()
            );
        } catch (Exception e) {
            log.error("구독 알림 발행 실패: type={}, err={}", event != null ? event.getType() : null, e.getMessage());
        }
    }
}
