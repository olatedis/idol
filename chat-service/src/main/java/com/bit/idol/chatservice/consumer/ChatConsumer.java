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
            if ("IDOL".equals(chatMessage.getSenderRole())) {
                // 아이돌이 보냄 -> 전체 팬에게 브로드캐스팅
                redisTemplate.convertAndSend("/sub/idol/" + chatMessage.getIdolId(), chatMessage);
                
            } else {
                // 팬이 보냄 -> 아이돌에게만 전송
                redisTemplate.convertAndSend("/queue/idol/" + chatMessage.getIdolId(), chatMessage);
            }
            
            log.debug("Kafka -> Redis Pub/Sub 전달 완료: room={}", chatMessage.getIdolId());

        } catch (Exception e) {
            log.error("채팅 메시지 소비 중 오류 발생: {}", e.getMessage());
        }
    }
}
