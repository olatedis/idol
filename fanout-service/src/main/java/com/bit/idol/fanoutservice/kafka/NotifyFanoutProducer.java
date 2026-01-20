package com.bit.idol.fanoutservice.kafka;

import com.bit.idol.fanoutservice.dto.NotifyFanoutEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// notify-fanout-topic으로 유저 단위 이벤트 발행
@Component
@RequiredArgsConstructor
public class NotifyFanoutProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper om;

    public void send(String topic, NotifyFanoutEvent event) {
        try {
            String json = om.writeValueAsString(event);
            kafkaTemplate.send(topic, event.getEventId(), json);
        } catch (Exception ignore) {
        }
    }
}
