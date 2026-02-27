package com.bit.idol.chatservice.scheduler;

import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.bit.idol.chatservice.entity.ChatMessage;
import com.bit.idol.chatservice.producer.ChatProducer;
import com.bit.idol.chatservice.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageResender {

    private final ChatRepository chatRepository;
    private final ChatProducer chatProducer;

    // 10초마다 실행
    @Scheduled(fixedDelay = 10000)
    public void resendPendingMessages() {
        // 1분 이상 PENDING 상태인 메시지 조회
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        List<ChatMessage> pendingMessages = chatRepository.findByStatusAndCreatedAtBefore("PENDING", oneMinuteAgo);

        if (pendingMessages.isEmpty())
            return;

        log.info("재전송 대상 메시지 발견: {}건", pendingMessages.size());

        for (ChatMessage message : pendingMessages) {
            try {
                ChatMessageDto dto = convertToDto(message);
                chatProducer.sendChatMessage(dto);

                // 성공 시 상태 업데이트
                message.setStatus("SENT");
                chatRepository.save(message);

                log.info("메시지 재전송 성공: id={}", message.getId());
            } catch (Exception e) {
                log.error("메시지 재전송 실패: id={}, error={}", message.getId(), e.getMessage());
                // 실패 시 다음 스케줄에 다시 시도 (또는 retryCount 증가 후 Dead Letter Queue로 이동)
            }
        }
    }

    private ChatMessageDto convertToDto(ChatMessage entity) {
        return ChatMessageDto.builder()
                .id(entity.getId())
                .idolId(entity.getIdolId())
                .senderId(entity.getSenderId())
                .senderNickname(entity.getSenderNickname())
                .senderRole(entity.getSenderRole())
                .content(entity.getContent())
                .type(entity.getType())
                .thumbnailUrl(entity.getThumbnailUrl()) // 썸네일 경로 매핑 추가 (재전송 유실 방지)
                .parentId(entity.getParentId())
                .reactions(entity.getReactions())
                .build();
    }
}
