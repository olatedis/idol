package com.bit.idol.fanoutservice.kafka;

import com.bit.idol.fanoutservice.dto.NotifyEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotifyFanoutProducer {

    private final KafkaTemplate<String, NotifyEvent> kafkaTemplate;

    @Value("${fanout.topics.fanout}")
    private String fanoutTopic;

    public void send(NotifyEvent event) {
        // key는 eventId를 쓰면 파티셔닝/순서에 도움이 됨
        kafkaTemplate.send(fanoutTopic, event.getEventId(), event);
    }
}
