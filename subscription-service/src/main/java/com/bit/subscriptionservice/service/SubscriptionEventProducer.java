package com.bit.subscriptionservice.service;

import com.bit.subscriptionservice.dto.SubscriptionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * subscription-service 알림 발행 전용 Producer
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionEventProducer {

    @Value("${spring.kafka.topic.notify-request}")
    private String notifyTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // 기존 메서드 (기본 토픽 사용)
    public void publish(SubscriptionEvent event) {
        publish(notifyTopic, event);
    }

    // 오버로딩 메서드 (토픽 지정 가능)
    public void publish(String topic, SubscriptionEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, json);

            log.info("구독 알림 발행: topic={}, type={}, targetType={}, targetId={}",
                    topic,
                    event.getType(),
                    event.getTargetType(),
                    event.getTargetId()
            );
        } catch (Exception e) {
            log.error("구독 알림 발행 실패: type={}, err={}", event != null ? event.getType() : null, e.getMessage());
        }
    }
}
