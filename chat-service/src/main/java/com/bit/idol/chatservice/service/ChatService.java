package com.bit.idol.chatservice.service;

import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.bit.idol.chatservice.dto.notification.NotificationEventDto;
import com.bit.idol.chatservice.dto.notification.TargetType;
import com.bit.idol.chatservice.entity.ChatMessage;
import com.bit.idol.chatservice.filter.ChatFilter;
import com.bit.idol.chatservice.filter.SuspiciousWordFilter;
import com.bit.idol.chatservice.producer.ChatProducer;
import com.bit.idol.chatservice.producer.NotificationProducer;
import com.bit.idol.chatservice.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatFilter chatFilter;
    private final SuspiciousWordFilter suspiciousWordFilter;
    private final ChatProducer chatProducer;
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationProducer notificationProducer;

    private static final long RATE_LIMIT_SECONDS = 3;
    private static final int CACHE_SIZE = 50;

    public void processMessage(ChatMessageDto messageDto) {
        // 0. 도배 방지
        if ("USER".equals(messageDto.getSenderRole())) {
            String limitKey = "chat:limit:" + messageDto.getSenderId();
            if (redisTemplate.hasKey(limitKey)) {
                throw new RuntimeException("메시지를 너무 빠르게 보낼 수 없습니다. 잠시 후 다시 시도해주세요.");
            }
            redisTemplate.opsForValue().set(limitKey, "1", Duration.ofSeconds(RATE_LIMIT_SECONDS));
        }

        try {
            // 1. 메시지 검열
            String filteredContent = chatFilter.filter(messageDto.getContent());
            messageDto.setContent(filteredContent);
        } catch (RuntimeException e) {
            log.warn("욕설 감지됨. 유저 신고 처리 (Kafka): userId={}, msg={}", messageDto.getSenderId(), e.getMessage());
            chatProducer.sendReport(messageDto.getSenderId());
            throw e;
        }

        // 2. MongoDB 저장
        ChatMessage chatMessage = ChatMessage.builder()
                .idolId(messageDto.getIdolId())
                .senderId(messageDto.getSenderId())
                .senderNickname(messageDto.getSenderNickname())
                .senderRole(messageDto.getSenderRole())
                .content(messageDto.getContent())
                .type(messageDto.getType())
                .parentId(messageDto.getParentId())
                .createdAt(LocalDateTime.now())
                .build();
        
        ChatMessage savedMessage = chatRepository.save(chatMessage);
        
        messageDto.setId(savedMessage.getId());

        // 3. Redis 캐싱 (최신 메시지 목록)
        String cacheKey = "chat:room:" + messageDto.getIdolId();
        redisTemplate.opsForList().leftPush(cacheKey, messageDto);
        redisTemplate.opsForList().trim(cacheKey, 0, CACHE_SIZE - 1);

        // 3-1. Redis 미리보기 캐싱
        String previewKey = "chat:preview:" + messageDto.getIdolId();
        Map<String, Object> previewData = new HashMap<>();
        previewData.put("content", messageDto.getContent());
        previewData.put("sender", messageDto.getSenderNickname());
        previewData.put("time", LocalDateTime.now().toString());
        redisTemplate.opsForValue().set(previewKey, previewData);

        // 4. Kafka로 전송
        chatProducer.sendChatMessage(messageDto);
        
        log.info("메시지 처리 완료: room={}, id={}", messageDto.getIdolId(), savedMessage.getId());

        // 5. 알림 발송
        sendNotification(messageDto);

        // 6. AI 비동기 검사
        if (suspiciousWordFilter.isSuspicious(messageDto.getContent())) {
            log.info("의심 메시지 감지 -> AI 검사 요청: msgId={}", messageDto.getId());
            chatProducer.sendAiCheck(messageDto);
        }
    }

    // 미리보기 조회
    public Map<String, Object> getChatPreview(Long idolId) {
        String previewKey = "chat:preview:" + idolId;
        Object data = redisTemplate.opsForValue().get(previewKey);
        
        if (data != null) {
            return (Map<String, Object>) data;
        }
        
        Pageable pageable = PageRequest.of(0, 1);
        List<ChatMessage> messages = chatRepository.findByIdolIdOrderByIdDesc(idolId, pageable);
        
        if (!messages.isEmpty()) {
            ChatMessage lastMsg = messages.get(0);
            Map<String, Object> preview = new HashMap<>();
            preview.put("content", lastMsg.getContent());
            preview.put("sender", lastMsg.getSenderNickname());
            preview.put("time", lastMsg.getCreatedAt().toString());
            
            redisTemplate.opsForValue().set(previewKey, preview);
            return preview;
        }
        
        return null;
    }

    // --- 공지사항 (Pinned Message) ---

    public void pinMessage(String messageId, Long idolId) {
        ChatMessage message = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("메시지를 찾을 수 없습니다."));

        // Redis에 저장 (덮어쓰기)
        String pinKey = "chat:pin:" + idolId;
        ChatMessageDto pinDto = convertToDto(message);
        redisTemplate.opsForValue().set(pinKey, pinDto);
        
        // 실시간 알림 (PIN 타입으로 전송)
        pinDto.setType("PIN");
        chatProducer.sendChatMessage(pinDto);

        log.info("공지사항 등록 완료: room={}, msgId={}", idolId, messageId);
    }

    public void unpinMessage(Long idolId) {
        String pinKey = "chat:pin:" + idolId;
        redisTemplate.delete(pinKey);
        
        // 실시간 알림 (UNPIN 타입으로 전송)
        ChatMessageDto unpinEvent = ChatMessageDto.builder()
                .idolId(idolId)
                .type("UNPIN")
                .build();
        chatProducer.sendChatMessage(unpinEvent);
        
        log.info("공지사항 해제 완료: room={}", idolId);
    }

    public ChatMessageDto getPinnedMessage(Long idolId) {
        String pinKey = "chat:pin:" + idolId;
        Object data = redisTemplate.opsForValue().get(pinKey);
        
        if (data instanceof ChatMessageDto) {
            return (ChatMessageDto) data;
        }
        return null;
    }

    // --------------------------------

    private void sendNotification(ChatMessageDto messageDto) {
        try {
            if ("IDOL".equals(messageDto.getSenderRole())) {
                Map<String, String> args = new HashMap<>();
                args.put("idolName", messageDto.getSenderNickname());
                args.put("message", messageDto.getContent());

                NotificationEventDto event = NotificationEventDto.builder()
                        .eventId(UUID.randomUUID().toString())
                        .type("IDOL_MESSAGE")
                        .targetType(TargetType.IDOL_SUB)
                        .targetId(String.valueOf(messageDto.getIdolId()))
                        .args(args)
                        .redirectUrl("/chat/room/" + messageDto.getIdolId())
                        .occurredAt(LocalDateTime.now())
                        .build();
                
                notificationProducer.send(event);
            }

            if (messageDto.getParentId() != null && !messageDto.getParentId().isEmpty()) {
                chatRepository.findById(messageDto.getParentId()).ifPresent(parentMsg -> {
                    if (parentMsg.getSenderId() != messageDto.getSenderId()) {
                        Map<String, String> args = new HashMap<>();
                        args.put("replierName", messageDto.getSenderNickname());
                        
                        NotificationEventDto event = NotificationEventDto.builder()
                                .eventId(UUID.randomUUID().toString())
                                .type("REPLY_MESSAGE")
                                .targetType(TargetType.USER)
                                .targetId(String.valueOf(parentMsg.getSenderId()))
                                .args(args)
                                .redirectUrl("/chat/room/" + messageDto.getIdolId())
                                .occurredAt(LocalDateTime.now())
                                .build();
                        
                        notificationProducer.send(event);
                    }
                });
            }
        } catch (Exception e) {
            log.error("알림 발송 중 오류: {}", e.getMessage());
        }
    }

    public void addReaction(String messageId, String reactionType, Long idolId) {
        ChatMessage message = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("메시지를 찾을 수 없습니다."));

        Map<String, Integer> reactions = message.getReactions();
        if (reactions == null) {
            reactions = new HashMap<>();
        }
        reactions.put(reactionType, reactions.getOrDefault(reactionType, 0) + 1);
        
        ChatMessage updatedMessage = ChatMessage.builder()
                .id(message.getId())
                .idolId(message.getIdolId())
                .senderId(message.getSenderId())
                .senderNickname(message.getSenderNickname())
                .senderRole(message.getSenderRole())
                .content(message.getContent())
                .type(message.getType())
                .parentId(message.getParentId())
                .reactions(reactions)
                .createdAt(message.getCreatedAt())
                .build();

        chatRepository.save(updatedMessage);

        ChatMessageDto reactionEvent = ChatMessageDto.builder()
                .id(messageId)
                .idolId(idolId)
                .type("REACTION")
                .reactions(reactions)
                .build();
        
        chatProducer.sendChatMessage(reactionEvent);
        
        log.info("반응 추가 완료: msgId={}, type={}", messageId, reactionType);
    }

    public void deleteMessage(String messageId, Long idolId, int userId) {
        ChatMessage message = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("메시지를 찾을 수 없습니다."));

        if (message.getSenderId() != userId) {
            throw new RuntimeException("본인의 메시지만 삭제할 수 있습니다.");
        }

        ChatMessage deletedMessage = ChatMessage.builder()
                .id(message.getId())
                .idolId(message.getIdolId())
                .senderId(message.getSenderId())
                .senderNickname(message.getSenderNickname())
                .senderRole(message.getSenderRole())
                .content("삭제된 메시지입니다.")
                .type("DELETED")
                .parentId(message.getParentId())
                .reactions(message.getReactions())
                .createdAt(message.getCreatedAt())
                .build();
        
        chatRepository.save(deletedMessage);

        ChatMessageDto deleteEvent = ChatMessageDto.builder()
                .id(messageId)
                .idolId(idolId)
                .type("DELETE")
                .build();
        
        chatProducer.sendChatMessage(deleteEvent);
        
        log.info("메시지 삭제 처리 완료: id={}", messageId);
    }

    public boolean isIdolOnline(Long idolId) {
        String onlineKey = "idol:online:" + idolId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(onlineKey));
    }

    public void setIdolOnline(Long idolId, boolean isOnline) {
        String onlineKey = "idol:online:" + idolId;
        if (isOnline) {
            redisTemplate.opsForValue().set(onlineKey, "ON");
        } else {
            redisTemplate.delete(onlineKey);
        }
    }

    public List<ChatMessageDto> getChatHistory(Long idolId, String lastId, int size) {
        if (lastId == null) {
            String cacheKey = "chat:room:" + idolId;
            List<Object> cachedMessages = redisTemplate.opsForList().range(cacheKey, 0, size - 1);
            
            if (cachedMessages != null && !cachedMessages.isEmpty()) {
                return cachedMessages.stream()
                        .map(obj -> (ChatMessageDto) obj)
                        .collect(Collectors.toList());
            }
        }

        Pageable pageable = PageRequest.of(0, size);
        List<ChatMessage> messages;

        if (lastId == null) {
            messages = chatRepository.findByIdolIdOrderByIdDesc(idolId, pageable);
        } else {
            messages = chatRepository.findByIdolIdAndIdLessThanOrderByIdDesc(idolId, lastId, pageable);
        }
        
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
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
                .parentId(entity.getParentId())
                .reactions(entity.getReactions())
                .build();
    }
}
