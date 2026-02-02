package com.bit.idol.chatservice.service;

import com.bit.idol.chatservice.client.SubscriptionFeignClient;
import com.bit.idol.chatservice.client.UserFeignClient;
import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.bit.idol.chatservice.dto.ChatRoomListDto;
import com.bit.idol.chatservice.dto.IdolDto;
import com.bit.idol.chatservice.dto.SubscriptionDto;
import com.bit.idol.chatservice.dto.notification.NotificationEventDto;
import com.bit.idol.chatservice.dto.notification.TargetType;
import com.bit.idol.chatservice.entity.ChatMessage;
import com.bit.idol.chatservice.filter.ChatFilter;
import com.bit.idol.chatservice.filter.SuspiciousWordFilter;
import com.bit.idol.chatservice.producer.ChatProducer;
import com.bit.idol.chatservice.producer.NotificationProducer;
import com.bit.idol.chatservice.repository.ChatRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
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
    private final UserFeignClient userFeignClient;
    private final SubscriptionFeignClient subscriptionFeignClient;
    private final ObjectMapper objectMapper; // 추가됨

    private static final long RATE_LIMIT_SECONDS = 3;
    private static final int CACHE_SIZE = 50;

    // 채팅방 목록 조회 (Aggregation) - 장애 격리 적용
    @CircuitBreaker(name = "chat-room-list", fallbackMethod = "getChatRoomListFallback")
    public List<ChatRoomListDto> getChatRoomList(int userId) {
        // 1. 전체 아이돌 목록 조회 (User Service)
        List<IdolDto> idols = userFeignClient.getAllIdols();

        // 2. 내 구독 목록 조회 (Subscription Service)
        // 구독 서비스 장애 시 빈 목록으로 처리 (채팅방 목록은 보여줘야 함)
        Set<Integer> subscribedIdolIds = new HashSet<>();
        try {
            List<SubscriptionDto> mySubscriptions = subscriptionFeignClient.getMySubscriptions(userId);
            subscribedIdolIds = mySubscriptions.stream()
                    .map(SubscriptionDto::getIdolId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("구독 정보 조회 실패 (Fallback: 구독 정보 없이 목록 표시): {}", e.getMessage());
        }

        // 3. 데이터 조합
        Set<Integer> finalSubscribedIdolIds = subscribedIdolIds;
        return idols.stream().map(idol -> {
            Long idolId = (long) idol.getIdolId();
            
            // 마지막 메시지 조회 (Redis 캐시 활용)
            Map<String, Object> preview = getChatPreview(idolId);
            String lastMessage = "";
            LocalDateTime lastMessageTime = null;

            if (preview != null) {
                lastMessage = (String) preview.get("content");
                String timeStr = (String) preview.get("time");
                if (timeStr != null) {
                    lastMessageTime = LocalDateTime.parse(timeStr);
                }
            }

            return ChatRoomListDto.builder()
                    .idolId(idolId)
                    .idolName(idol.getStageName())
                    .thumbnailUrl(idol.getProfileImage())
                    .lastMessage(lastMessage)
                    .lastMessageTime(lastMessageTime)
                    .unreadCount(0)
                    .isSubscribed(finalSubscribedIdolIds.contains(idol.getIdolId()))
                    .build();
        }).collect(Collectors.toList());
    }

    // Fallback: User Service 장애 시 빈 목록 반환 (또는 캐시된 목록)
    public List<ChatRoomListDto> getChatRoomListFallback(int userId, Throwable t) {
        log.error("채팅방 목록 조회 실패 (User Service 장애): {}", t.getMessage());
        return Collections.emptyList(); // 일단 빈 목록 반환 (프론트에서 재시도 유도)
    }

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

        // 2. MongoDB 저장 (PENDING 상태)
        ChatMessage chatMessage = ChatMessage.builder()
                .idolId(messageDto.getIdolId())
                .senderId(messageDto.getSenderId())
                .senderNickname(messageDto.getSenderNickname())
                .senderRole(messageDto.getSenderRole())
                .content(messageDto.getContent())
                .type(messageDto.getType())
                .parentId(messageDto.getParentId())
                .createdAt(LocalDateTime.now())
                .status("PENDING") // Outbox Pattern
                .build();
        
        ChatMessage savedMessage = chatRepository.save(chatMessage);
        
        messageDto.setId(savedMessage.getId());

        // 3. Redis 캐싱
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

        // 4. Kafka로 전송 (성공 시 SENT 업데이트)
        try {
            chatProducer.sendChatMessage(messageDto);
            
            // 전송 성공 -> 상태 업데이트
            savedMessage.setStatus("SENT");
            chatRepository.save(savedMessage);
            
            log.info("메시지 처리 완료: room={}, id={}", messageDto.getIdolId(), savedMessage.getId());
        } catch (Exception e) {
            log.error("Kafka 전송 실패 (재전송 대기): {}", e.getMessage());
            // 예외를 던지지 않고 PENDING 상태로 둠 -> 스케줄러가 처리
        }

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

        String pinKey = "chat:pin:" + idolId;
        ChatMessageDto pinDto = convertToDto(message);
        redisTemplate.opsForValue().set(pinKey, pinDto);
        
        pinDto.setType("PIN");
        chatProducer.sendChatMessage(pinDto);

        log.info("공지사항 등록 완료: room={}, msgId={}", idolId, messageId);
    }

    public void unpinMessage(Long idolId) {
        String pinKey = "chat:pin:" + idolId;
        redisTemplate.delete(pinKey);
        
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
        
        if (data != null) {
            return objectMapper.convertValue(data, ChatMessageDto.class);
        }
        return null;
    }

    // --- 미디어 모아보기 ---

    public List<ChatMessageDto> getChatMedia(Long idolId, String lastId, int size) {
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "id"));
        List<ChatMessage> messages;

        if (lastId == null) {
            messages = chatRepository.findMediaByIdolId(idolId, pageable);
        } else {
            messages = chatRepository.findMediaByIdolIdAndIdLessThan(idolId, lastId, pageable);
        }

        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
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
                        .map(obj -> objectMapper.convertValue(obj, ChatMessageDto.class)) // 안전한 변환 적용
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
