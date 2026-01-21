package com.bit.idol.chatservice.service;

import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.bit.idol.chatservice.entity.ChatMessage;
import com.bit.idol.chatservice.filter.ChatFilter;
import com.bit.idol.chatservice.producer.ReportProducer;
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
    private final ReportProducer reportProducer;

    public void processMessage(ChatMessageDto messageDto) {
        try {
            // 1. 메시지 검열
            String filteredContent = chatFilter.filter(messageDto.getContent());
            messageDto.setContent(filteredContent);
        } catch (RuntimeException e) {
            // 욕설 감지 시 예외 발생
            log.warn("욕설 감지됨. 유저 신고 처리 (Kafka): userId={}, msg={}", messageDto.getSenderId(), e.getMessage());
            
            // 유저 신고 (Kafka 비동기 전송)
            reportProducer.sendReport(messageDto.getSenderId());
            
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

        // 3. Redis Pub/Sub 발행 (라우팅 로직)
        if ("IDOL".equals(messageDto.getSenderRole())) {
            // 아이돌이 보냄 -> 전체 팬에게 브로드캐스팅 (/sub/idol/{id})
            redisTemplate.convertAndSend("/sub/idol/" + messageDto.getIdolId(), messageDto);
            
        } else {
            // 팬이 보냄 -> 아이돌에게만 전송 (/queue/idol/{id})
            redisTemplate.convertAndSend("/queue/idol/" + messageDto.getIdolId(), messageDto);
        }
        
        log.info("메시지 전송 완료: room={}, sender={}", messageDto.getIdolId(), messageDto.getSenderNickname());
    }
}
