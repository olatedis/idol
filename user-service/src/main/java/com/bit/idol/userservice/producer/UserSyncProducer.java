package com.bit.idol.userservice.producer;

import com.bit.idol.userservice.dto.kafka.UserEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC = "user-update-topic";

    public void send(int userId, String type) {
        try {
            UserEventDto event = UserEventDto.builder()
                    .userId(userId)
                    .type(type)
                    .build();
            
            String jsonMessage = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, jsonMessage);
            log.info("유저 동기화 이벤트 발행: userId={}, type={}", userId, type);
        } catch (Exception e) {
            log.error("유저 동기화 이벤트 발행 실패: {}", e.getMessage());
            // 여기서 실패하면 MongoDB 동기화가 안 되므로, 
            // 실제 운영 환경에서는 별도의 백업 테이블(Outbox)에 저장하거나 재시도 로직이 필요함.
        }
    }
}
