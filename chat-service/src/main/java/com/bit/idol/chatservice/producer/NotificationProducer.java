package com.bit.idol.chatservice.producer;

import com.bit.idol.chatservice.dto.notification.NotificationEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC = "notify-request-topic";

    public void send(NotificationEventDto event) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, jsonMessage);
            log.info("알림 요청 이벤트 발행 성공: topic={}, type={}, targetId={}", TOPIC, event.getType(), event.getTargetId());
        } catch (Exception e) {
            log.error("알림 요청 이벤트 발행 실패: {}", e.getMessage());
        }
    }
}
