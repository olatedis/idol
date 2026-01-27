package com.bit.idol.chatservice.producer;

import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String CHAT_TOPIC = "chat-message-topic";
    private static final String REPORT_TOPIC = "user-report-topic";
    private static final String AI_CHECK_TOPIC = "chat-ai-check-topic"; // 추가됨

    // 채팅 메시지 전송 (Kafka)
    public void sendChatMessage(ChatMessageDto messageDto) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(messageDto);
            // 파티셔닝을 위해 idolId를 키로 사용 (순서 보장)
            kafkaTemplate.send(CHAT_TOPIC, String.valueOf(messageDto.getIdolId()), jsonMessage);
            log.debug("채팅 메시지 Kafka 전송: room={}", messageDto.getIdolId());
        } catch (JsonProcessingException e) {
            log.error("채팅 메시지 JSON 변환 실패", e);
        }
    }

    // 신고 메시지 전송
    public void sendReport(int userId) {
        String message = String.valueOf(userId);
        kafkaTemplate.send(REPORT_TOPIC, message);
        log.info("신고 메시지 Kafka 전송: userId={}", userId);
    }

    // AI 검사 요청 전송 (추가됨)
    public void sendAiCheck(ChatMessageDto messageDto) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(messageDto);
            kafkaTemplate.send(AI_CHECK_TOPIC, jsonMessage);
            log.debug("AI 검사 요청 Kafka 전송: msgId={}", messageDto.getId());
        } catch (JsonProcessingException e) {
            log.error("AI 검사 요청 JSON 변환 실패", e);
        }
    }
}
