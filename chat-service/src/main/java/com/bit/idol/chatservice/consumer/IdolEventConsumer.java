package com.bit.idol.chatservice.consumer;

import com.bit.idol.chatservice.dto.IdolDto;
import com.bit.idol.chatservice.dto.event.IdolEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdolEventConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String IDOL_CACHE_KEY = "idols:cache";

    @KafkaListener(topics = "idol-events", groupId = "chat-service-group")
    public void consume(String message) {
        try {
            IdolEvent event = objectMapper.readValue(message, IdolEvent.class);
            log.info("아이돌 이벤트 수신: type={}, idolId={}", event.getType(), event.getIdolId());

            IdolDto idolDto = IdolDto.builder()
                    .idolId(event.getIdolId())
                    .stageName(event.getStageName())
                    .profileImage(event.getProfileImage())
                    .status(event.getStatus())
                    .build();

            // Redis Hash에 저장 (idolId -> IdolDto)
            redisTemplate.opsForHash().put(IDOL_CACHE_KEY, String.valueOf(event.getIdolId()), idolDto);

        } catch (Exception e) {
            log.error("아이돌 이벤트 처리 실패: {}", e.getMessage());
        }
    }
}
