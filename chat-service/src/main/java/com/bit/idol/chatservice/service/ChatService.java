package com.bit.idol.chatservice.service;

import com.bit.idol.chatservice.client.UserFeignClient;
import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.bit.idol.chatservice.entity.ChatMessage;
import com.bit.idol.chatservice.filter.ChatFilter;
import com.bit.idol.chatservice.producer.ChatProducer;
import com.bit.idol.chatservice.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatFilter chatFilter;
    private final ChatProducer chatProducer;
    private final RedisTemplate<String, Object> redisTemplate;

    // 도배 방지 설정 (3초에 1회)
    private static final long RATE_LIMIT_SECONDS = 3;
    // Redis 캐싱 개수 (최신 50개)
    private static final int CACHE_SIZE = 50;

    public void processMessage(ChatMessageDto messageDto) {
        // 0. 도배 방지 (USER인 경우만)
        if ("USER".equals(messageDto.getSenderRole())) {
            String limitKey = "chat:limit:" + messageDto.getSenderId();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
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
                .parentId(messageDto.getParentId()) // 답장 기능
                .createdAt(LocalDateTime.now())
                .build();
        
        ChatMessage savedMessage = chatRepository.save(chatMessage);
        
        // DTO에 저장된 ID(MongoDB ID) 세팅
        messageDto.setId(savedMessage.getId());

        // 3. Redis 캐싱 (최신 메시지 저장)
        String cacheKey = "chat:room:" + messageDto.getIdolId();
        redisTemplate.opsForList().leftPush(cacheKey, messageDto);
        redisTemplate.opsForList().trim(cacheKey, 0, CACHE_SIZE - 1);

        // 4. Kafka로 전송
        chatProducer.sendChatMessage(messageDto);
        
        log.info("메시지 처리 완료: room={}, id={}", messageDto.getIdolId(), savedMessage.getId());
    }

    // 메시지 반응 추가 (좋아요, 하트 등)
    public void addReaction(String messageId, String reactionType, Long idolId) {
        // 1. 메시지 조회
        ChatMessage message = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("메시지를 찾을 수 없습니다."));

        // 2. 반응 카운트 증가
        Map<String, Integer> reactions = message.getReactions();
        if (reactions == null) {
            reactions = new HashMap<>();
        }
        reactions.put(reactionType, reactions.getOrDefault(reactionType, 0) + 1);
        
        // 3. DB 저장 (실제로는 MongoDB $inc 연산자가 더 효율적이지만 간단하게 구현)
        // 엔티티에 setter가 없으므로 builder로 새로 생성하거나, 엔티티에 메서드 추가 필요
        // 여기서는 편의상 repository.save로 덮어쓰기 위해 엔티티를 수정해야 함.
        // 하지만 ChatMessage는 @Builder만 있고 @Setter가 없으므로, 
        // reactions 필드는 Mutable Map이므로 직접 수정 후 save 가능.
        
        // 주의: reactions 필드가 null이면 위에서 생성했으므로, 다시 set 해줘야 함.
        // ChatMessage에 @Setter가 없으므로, 리플렉션이나 다시 빌드해야 함.
        // 가장 깔끔한 건 ChatMessage에 'addReaction' 메서드를 만드는 것.
        // 일단은 다시 빌드해서 저장.
        
        ChatMessage updatedMessage = ChatMessage.builder()
                .id(message.getId())
                .idolId(message.getIdolId())
                .senderId(message.getSenderId())
                .senderNickname(message.getSenderNickname())
                .senderRole(message.getSenderRole())
                .content(message.getContent())
                .type(message.getType())
                .parentId(message.getParentId())
                .reactions(reactions) // 업데이트된 반응
                .createdAt(message.getCreatedAt())
                .build();

        chatRepository.save(updatedMessage);

        // 4. 실시간 전송 (WebSocket)
        // { "type": "REACTION", "id": "msgId", "reactions": { "HEART": 11 } }
        ChatMessageDto reactionEvent = ChatMessageDto.builder()
                .id(messageId)
                .idolId(idolId)
                .type("REACTION")
                .reactions(reactions)
                .build();
        
        chatProducer.sendChatMessage(reactionEvent);
        
        log.info("반응 추가 완료: msgId={}, type={}", messageId, reactionType);
    }

    // 메시지 회수 (Soft Delete)
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

    // 아이돌 접속 상태 조회
    public boolean isIdolOnline(Long idolId) {
        String onlineKey = "idol:online:" + idolId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(onlineKey));
    }

    // 아이돌 접속 상태 변경
    public void setIdolOnline(Long idolId, boolean isOnline) {
        String onlineKey = "idol:online:" + idolId;
        if (isOnline) {
            redisTemplate.opsForValue().set(onlineKey, "ON");
        } else {
            redisTemplate.delete(onlineKey);
        }
    }

    // 채팅 내역 조회
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
