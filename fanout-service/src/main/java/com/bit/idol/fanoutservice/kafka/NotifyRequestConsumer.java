package com.bit.idol.fanoutservice.kafka;

import com.bit.idol.fanoutservice.dto.NotifyEvent;
import com.bit.idol.fanoutservice.service.FanoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyRequestConsumer {

    private final FanoutService fanoutService;

    @KafkaListener(
            topics = "${fanout.topics.request}",
            groupId = "fanout-service-group"
    )
    public void consume(NotifyEvent event) {
        if (event == null) return;
        log.info("notify-request consume. eventId={} eventType={} producer={}",
                event.getEventId(), event.getEventType(), event.getProducer());

        fanoutService.handle(event);
    }
}
