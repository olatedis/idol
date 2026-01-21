package com.bit.idol.chatservice.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "user-report-topic";

    public void sendReport(int userId) {
        String message = String.valueOf(userId);
        kafkaTemplate.send(TOPIC, message);
        log.info("신고 메시지 전송 (Kafka): userId={}", userId);
    }
}
