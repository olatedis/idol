package com.bit.idol.fanoutservice.kafka;

import com.bit.idol.fanoutservice.dto.NotifyFanoutEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// notify-fanout-topic으로 유저 단위 이벤트 발행
@Component
@RequiredArgsConstructor
@Slf4j
public class NotifyFanoutProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper om;

    public void send(String topic, NotifyFanoutEvent event) {
        try {
            String json = om.writeValueAsString(event);

            log.info("[FANOUT][PRODUCE] topic={}, eventId={}, targetId={}",
                    topic,
                    event.getEventId(),
                    event.getTargetId()
            );

            kafkaTemplate.send(topic, event.getEventId(), json);

        } catch (Exception e) {
            log.error("[FANOUT][PRODUCE ERROR] eventId={}, error={}",
                    event != null ? event.getEventId() : null,
                    e.getMessage(),
                    e
            );
        }
    }
}