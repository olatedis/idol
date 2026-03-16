package com.bit.idol.chatservice.service;

import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    // Redis에서 메시지가 오면 실행됨
    public void sendMessage(String publishMessage) {
        try {
            // JSON -> DTO 변환
            ChatMessageDto chatMessage = objectMapper.readValue(publishMessage, ChatMessageDto.class);

            // WebSocket으로 전송 (라우팅)
            if ("IDOL".equals(chatMessage.getSenderRole()) || "STATUS".equals(chatMessage.getType())
                    || "DELETE".equals(chatMessage.getType()) || "ADMIN".equals(chatMessage.getSenderRole())) {
                // 아이돌 메시지나 시스템/상태 알림/삭제 이벤트 -> /sub/idol/{id} 공용 채널 구독자들에게 전송
                messagingTemplate.convertAndSend("/sub/idol/" + chatMessage.getIdolId(), chatMessage);

            } else {
                // 팬 메시지 -> /queue/idol/{id} 구독자(아이돌) 예약 큐로 전송
                messagingTemplate.convertAndSend("/queue/idol/" + chatMessage.getIdolId(), chatMessage);
            }

            log.info("Redis Sub -> WebSocket 전송 완료: room={}, type={}", chatMessage.getIdolId(), chatMessage.getType());

        } catch (Exception e) {
            log.error("메시지 수신 중 오류 발생: {}", e.getMessage());
        }
    }
}
