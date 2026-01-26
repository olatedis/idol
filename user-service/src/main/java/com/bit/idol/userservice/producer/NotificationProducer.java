package com.bit.idol.userservice.producer;

import com.bit.idol.userservice.dto.notification.NotificationEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "notify-request-topic";

    public void send(NotificationEventDto event) {
        try {
            kafkaTemplate.send(TOPIC, event);
            log.info("알림 요청 이벤트 발행 성공: topic={}, type={}, targetId={}", TOPIC, event.getType(), event.getTargetId());
        } catch (Exception e) {
            log.error("알림 요청 이벤트 발행 실패: {}", e.getMessage());
        }
    }
}
