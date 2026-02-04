package com.bit.idol.boardservice.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostIndexProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    // board-post-index-topic 으로 색인 이벤트 발행
    public void send(String message) {
        kafkaTemplate.send("board-post-index-topic", message);
    }
}
