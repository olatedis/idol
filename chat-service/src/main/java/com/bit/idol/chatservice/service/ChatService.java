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
        
        // DTO에 저장된 ID(MongoDB ID) 세팅 (클라이언트가 정렬할 때 필요)
        messageDto.setId(savedMessage.getId());

        // 3. Redis 캐싱 (최신 메시지 저장)
        // Key: chat:room:{idolId}
        String cacheKey = "chat:room:" + messageDto.getIdolId();
        redisTemplate.opsForList().leftPush(cacheKey, messageDto);
        redisTemplate.opsForList().trim(cacheKey, 0, CACHE_SIZE - 1); // 50개만 유지

        // 4. Kafka로 전송
        chatProducer.sendChatMessage(messageDto);
        
        log.info("메시지 처리 완료: room={}, id={}", messageDto.getIdolId(), savedMessage.getId());
    }

    // 채팅 내역 조회 (페이징 + 캐싱)
    public List<ChatMessageDto> getChatHistory(Long idolId, String lastId, int size) {
        // 1. 첫 페이지 요청(lastId == null)이면 Redis 캐시 확인
        if (lastId == null) {
            String cacheKey = "chat:room:" + idolId;
            List<Object> cachedMessages = redisTemplate.opsForList().range(cacheKey, 0, size - 1);
            
            if (cachedMessages != null && !cachedMessages.isEmpty()) {
                log.info("채팅 내역 Redis 캐시 조회: room={}, size={}", idolId, cachedMessages.size());
                return cachedMessages.stream()
                        .map(obj -> (ChatMessageDto) obj)
                        .collect(Collectors.toList());
            }
        }

        // 2. 캐시 없거나 더 과거 데이터 요청이면 MongoDB 조회
        Pageable pageable = PageRequest.of(0, size);
        List<ChatMessage> messages;

        if (lastId == null) {
            messages = chatRepository.findByIdolIdOrderByIdDesc(idolId, pageable);
        } else {
            messages = chatRepository.findByIdolIdAndIdLessThanOrderByIdDesc(idolId, lastId, pageable);
        }

        log.info("채팅 내역 DB 조회: room={}, lastId={}, size={}", idolId, lastId, messages.size());
        
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private ChatMessageDto convertToDto(ChatMessage entity) {
        return ChatMessageDto.builder()
                .id(entity.getId()) // MongoDB ID
                .idolId(entity.getIdolId())
                .senderId(entity.getSenderId())
                .senderNickname(entity.getSenderNickname())
                .senderRole(entity.getSenderRole())
                .content(entity.getContent())
                .type(entity.getType())
                .build();
    }
}
