package com.bit.idol.chatservice.service;

import com.bit.idol.chatservice.client.UserFeignClient;
import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.bit.idol.chatservice.entity.ChatMessage;
import com.bit.idol.chatservice.filter.ChatFilter;
import com.bit.idol.chatservice.producer.ChatProducer;
import com.bit.idol.chatservice.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

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

    public void processMessage(ChatMessageDto messageDto) {
        // 0. 도배 방지 (USER인 경우만)
        if ("USER".equals(messageDto.getSenderRole())) {
            String limitKey = "chat:limit:" + messageDto.getSenderId();
            
            // 키가 존재하면 도배로 간주
            if (redisTemplate.hasKey(limitKey)) {
                log.warn("도배 방지 걸림: userId={}", messageDto.getSenderId());
                throw new RuntimeException("메시지를 너무 빠르게 보낼 수 없습니다. 잠시 후 다시 시도해주세요.");
            }
            
            // 키 생성 (3초 후 만료)
            redisTemplate.opsForValue().set(limitKey, "1", Duration.ofSeconds(RATE_LIMIT_SECONDS));
        }

        try {
            // 1. 메시지 검열
            String filteredContent = chatFilter.filter(messageDto.getContent());
            messageDto.setContent(filteredContent);
        } catch (RuntimeException e) {
            // 욕설 감지 시 예외 발생
            log.warn("욕설 감지됨. 유저 신고 처리 (Kafka): userId={}, msg={}", messageDto.getSenderId(), e.getMessage());
            
            // 유저 신고 (Kafka 비동기 전송)
            chatProducer.sendReport(messageDto.getSenderId());
            
            // 클라이언트에게 에러 전파 (메시지 전송 중단)
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
        
        chatRepository.save(chatMessage);

        // 3. Kafka로 전송 (Redis로 직접 쏘지 않음!)
        chatProducer.sendChatMessage(messageDto);
        
        log.info("메시지 처리 완료 (DB저장 -> Kafka전송): room={}, sender={}", messageDto.getIdolId(), messageDto.getSenderNickname());
    }
}
