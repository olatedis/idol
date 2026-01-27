package com.bit.idol.chatservice.consumer;

import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.bit.idol.chatservice.service.AiFilterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiCheckConsumer {

    private final AiFilterService aiFilterService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "chat-ai-check-topic", groupId = "chat-ai-check-group")
    public void checkMessage(String message) {
        try {
            ChatMessageDto messageDto = objectMapper.readValue(message, ChatMessageDto.class);
            aiFilterService.check(messageDto); // 비동기 아님 (Kafka 자체가 비동기)
        } catch (Exception e) {
            log.error("AI 검사 메시지 처리 중 오류: {}", e.getMessage());
        }
    }
}
