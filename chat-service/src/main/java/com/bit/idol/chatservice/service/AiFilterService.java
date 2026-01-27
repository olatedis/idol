package com.bit.idol.chatservice.service;

import com.bit.idol.chatservice.client.OpenAiClient;
import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.bit.idol.chatservice.dto.openai.ModerationRequest;
import com.bit.idol.chatservice.dto.openai.ModerationResponse;
import com.bit.idol.chatservice.entity.ChatMessage;
import com.bit.idol.chatservice.producer.ChatProducer;
import com.bit.idol.chatservice.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiFilterService {

    private final ChatRepository chatRepository;
    private final ChatProducer chatProducer;
    private final OpenAiClient openAiClient;

    @Value("${openai.api-key}")
    private String apiKey;

    public void check(ChatMessageDto messageDto) {
        try {
            // OpenAI Moderation API 호출
            ModerationRequest request = new ModerationRequest(messageDto.getContent());
            ModerationResponse response = openAiClient.checkModeration("Bearer " + apiKey, request);

            if (response != null && !response.getResults().isEmpty()) {
                boolean isFlagged = response.getResults().get(0).isFlagged();

                if (isFlagged) {
                    log.warn("AI 필터링 적발! 메시지 삭제 및 제재 처리: msgId={}, content={}", messageDto.getId(), messageDto.getContent());
                    
                    // 1. 메시지 삭제 (Soft Delete)
                    chatRepository.findById(messageDto.getId()).ifPresent(message -> {
                        ChatMessage deletedMessage = ChatMessage.builder()
                                .id(message.getId())
                                .idolId(message.getIdolId())
                                .senderId(message.getSenderId())
                                .senderNickname(message.getSenderNickname())
                                .senderRole(message.getSenderRole())
                                .content("부적절한 메시지로 인해 AI에 의해 삭제되었습니다.")
                                .type("DELETED")
                                .parentId(message.getParentId())
                                .reactions(message.getReactions())
                                .createdAt(message.getCreatedAt())
                                .build();
                        
                        chatRepository.save(deletedMessage);

                        // 삭제 이벤트 전송
                        ChatMessageDto deleteEvent = ChatMessageDto.builder()
                                .id(message.getId())
                                .idolId(message.getIdolId())
                                .type("DELETE")
                                .build();
                        chatProducer.sendChatMessage(deleteEvent);
                    });

                    // 2. 유저 신고 (Kafka)
                    chatProducer.sendReport(messageDto.getSenderId());
                }
            }

        } catch (Exception e) {
            log.error("AI 필터링 중 오류 발생: {}", e.getMessage());
        }
    }
}
