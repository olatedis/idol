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

    private final ChatService chatService;
    private final ChatRepository chatRepository;
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
                    log.warn("AI 필터링 적발! 메시지 삭제 및 캐시 갱신: msgId={}, content={}", messageDto.getId(), messageDto.getContent());
                    
                    // ChatService의 공통 로직을 사용하여 DB + Redis + 실시간 알림을 한 번에 처리
                    chatRepository.findById(messageDto.getId()).ifPresent(message -> {
                        chatService.performDeletion(message, "부적절한 메시지로 인해 AI에 의해 삭제되었습니다.", "AI_FILTERED");
                    });

                    // 유저 신고 (기존 로직 유지)
                    chatService.sendReportEvent(messageDto.getSenderId());
                }
            }

        } catch (Exception e) {
            log.error("AI 필터링 중 오류 발생: {}", e.getMessage());
        }
    }
}
