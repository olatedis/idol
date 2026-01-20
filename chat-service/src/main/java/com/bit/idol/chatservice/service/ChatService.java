package com.bit.idol.chatservice.service;

import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.bit.idol.chatservice.entity.ChatMessage;
import com.bit.idol.chatservice.filter.ChatFilter;
import com.bit.idol.chatservice.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRepository chatRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatFilter chatFilter;

    public void processMessage(ChatMessageDto messageDto) {
        // 1. 메시지 검열
        String filteredContent = chatFilter.filter(messageDto.getContent());
        messageDto.setContent(filteredContent);

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

        // 3. Redis Pub/Sub 발행 (라우팅 로직)
        if ("IDOL".equals(messageDto.getSenderRole())) {
            // 아이돌이 보냄 -> 전체 팬에게 브로드캐스팅 (/sub/idol/{id})
            redisTemplate.convertAndSend("/sub/idol/" + messageDto.getIdolId(), messageDto);
            
        } else {
            // 팬이 보냄 -> 아이돌에게만 전송 (/queue/idol/{id})
            // 주의: 실제로는 아이돌이 접속 중인 서버로만 보내야 효율적이지만,
            // 간단하게 구현하기 위해 아이돌 전용 토픽으로 발행함.
            redisTemplate.convertAndSend("/queue/idol/" + messageDto.getIdolId(), messageDto);
        }
        
        log.info("메시지 전송 완료: room={}, sender={}", messageDto.getIdolId(), messageDto.getSenderNickname());
    }
}
