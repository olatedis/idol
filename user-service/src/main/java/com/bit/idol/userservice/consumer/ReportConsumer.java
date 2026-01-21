package com.bit.idol.userservice.consumer;

import com.bit.idol.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportConsumer {

    private final UserService userService;

    // Kafka에서 신고 메시지 수신
    @KafkaListener(topics = "user-report-topic", groupId = "user-service-group")
    public void consumeReport(String message) {
        try {
            int userId = Integer.parseInt(message);
            log.info("신고 메시지 수신 (Kafka): userId={}", userId);
            
            userService.increaseReportCount(userId);
            
        } catch (NumberFormatException e) {
            log.error("잘못된 신고 메시지 형식: {}", message);
        } catch (Exception e) {
            log.error("신고 처리 중 오류 발생: {}", e.getMessage());
        }
    }
}
