package com.bit.idol.userservice.producer;

import com.bit.idol.userservice.dto.notification.NotificationEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate; // Value 타입 String으로 변경
    private final ObjectMapper objectMapper; // JSON 변환기

    @Value("${spring.kafka.topic.notify-request}")
    private String topic;

    public void send(NotificationEventDto event) {
        try {
            // 객체 -> JSON 문자열 변환
            String jsonMessage = objectMapper.writeValueAsString(event);
            
            kafkaTemplate.send(topic, jsonMessage);
            log.info("알림 요청 이벤트 발행 성공: topic={}, type={}, targetId={}", topic, event.getType(), event.getTargetId());
        } catch (Exception e) {
            log.error("알림 요청 이벤트 발행 실패: {}", e.getMessage());
        }
    }
}
