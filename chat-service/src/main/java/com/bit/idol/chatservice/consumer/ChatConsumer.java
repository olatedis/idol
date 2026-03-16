package com.bit.idol.chatservice.consumer;

import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // Kafka에서 채팅 메시지 수신 -> Redis Pub/Sub으로 발행
    @KafkaListener(topics = "chat-message-topic", groupId = "chat-service-group")
    public void consumeChatMessage(String message) {
        try {
            // JSON -> DTO
            ChatMessageDto chatMessage = objectMapper.readValue(message, ChatMessageDto.class);

            // Redis Pub/Sub 발행 (라우팅 로직)
            // 삭제(DELETE) 이벤트는 무조건 전체 참여자에게 알려야 실시간 반영 및 알림이 가능함
            if ("IDOL".equals(chatMessage.getSenderRole()) || "DELETE".equals(chatMessage.getType())) {
                redisTemplate.convertAndSend("/sub/idol/" + chatMessage.getIdolId(), chatMessage);
            } else {
                // 일반 팬이 보낸 채팅 메시지는 아이돌에게만 전송
                redisTemplate.convertAndSend("/queue/idol/" + chatMessage.getIdolId(), chatMessage);
            }
            
            log.info("Kafka -> Redis Pub/Sub 전달 완료: room={}, type={}", chatMessage.getIdolId(), chatMessage.getType());

        } catch (Exception e) {
            log.error("채팅 메시지 소비 중 오류 발생: {}", e.getMessage());
        }
    }
}
