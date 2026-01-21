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
import java.util.List;
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

    // 메시지 회수 (Soft Delete)
    public void deleteMessage(String messageId, Long idolId, int userId) {
        // 1. DB에서 메시지 조회
        ChatMessage message = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("메시지를 찾을 수 없습니다."));

        // 2. 권한 확인 (본인 메시지인지, 아이돌인지)
        if (message.getSenderId() != userId) {
            throw new RuntimeException("본인의 메시지만 삭제할 수 있습니다.");
        }

        // 3. Soft Delete 처리 (내용 변경 및 타입 변경)
        // 실제로는 update를 해야 하므로 save를 다시 호출
        ChatMessage deletedMessage = ChatMessage.builder()
                .id(message.getId())
                .idolId(message.getIdolId())
                .senderId(message.getSenderId())
                .senderNickname(message.getSenderNickname())
                .senderRole(message.getSenderRole())
                .content("삭제된 메시지입니다.")
                .type("DELETED") // 타입 변경
                .createdAt(message.getCreatedAt())
                .build();
        
        chatRepository.save(deletedMessage);

        // 4. Redis 캐시에서도 삭제 (또는 업데이트)
        // 리스트에서 특정 요소를 찾아 지우는 건 복잡하므로, 
        // 간단하게 캐시 전체를 날려버리거나(다음 조회 시 DB에서 긁어옴), 
        // 삭제 이벤트를 전송해서 클라이언트가 처리하게 함.
        // 여기서는 "삭제 이벤트 전송"에 집중.

        // 5. 삭제 이벤트 브로드캐스팅 (Kafka -> Redis -> WebSocket)
        ChatMessageDto deleteEvent = ChatMessageDto.builder()
                .id(messageId)
                .idolId(idolId)
                .type("DELETE") // 클라이언트에게 "이거 지워!"라고 알림
                .build();
        
        chatProducer.sendChatMessage(deleteEvent);
        
        log.info("메시지 삭제 처리 완료: id={}", messageId);
    }

    // 아이돌 접속 상태 조회
    public boolean isIdolOnline(Long idolId) {
        String onlineKey = "idol:online:" + idolId;
        return redisTemplate.hasKey(onlineKey);
    }

    // 아이돌 접속 상태 변경 (StompHandler에서 호출)
    public void setIdolOnline(Long idolId, boolean isOnline) {
        String onlineKey = "idol:online:" + idolId;
        if (isOnline) {
            redisTemplate.opsForValue().set(onlineKey, "ON");
        } else {
            redisTemplate.delete(onlineKey);
        }
    }

    // 채팅 내역 조회 (페이징 + 캐싱)
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
                .build();
    }
}
