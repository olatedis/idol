package com.bit.idol.chatservice.consumer;

import com.bit.idol.chatservice.dto.event.UserEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserUpdateConsumer {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    @KafkaListener(topics = "user-update-topic", groupId = "chat-service-group")
    public void consumeUserUpdate(String message) {
        try {
            UserEventDto event = objectMapper.readValue(message, UserEventDto.class);
            log.info("유저 업데이트 이벤트 수신 (Kafka): userId={}, type={}, status={}", 
                    event.getUserId(), event.getType(), event.getStatus());

            Map<String, String> payload = new HashMap<>();
            
            if ("DELETE".equals(event.getType())) {
                payload.put("type", "WITHDRAWAL");
                payload.put("message", "회원 탈퇴가 완료되었습니다. 자동으로 로그아웃됩니다.");
                messagingTemplate.convertAndSend("/queue/errors/" + event.getUserId(), payload);
                log.info("유저 탈퇴 알림 전송: userId={}", event.getUserId());
            } else if ("UPDATE".equals(event.getType()) && "RESTRICTED".equals(event.getStatus())) {
                payload.put("type", "STATUS_UPDATE");
                payload.put("status", "RESTRICTED");
                payload.put("message", "활동이 제한되었습니다.");
                messagingTemplate.convertAndSend("/queue/errors/" + event.getUserId(), payload);
                log.info("유저 제재 알림 전송: userId={}", event.getUserId());
            }

        } catch (Exception e) {
            log.error("유저 업데이트 이벤트 처리 중 오류 발생: {}", e.getMessage());
        }
    }
}
