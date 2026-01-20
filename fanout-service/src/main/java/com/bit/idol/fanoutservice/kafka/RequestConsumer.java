package com.bit.idol.fanoutservice.kafka;

import com.bit.idol.fanoutservice.dto.NotifyRequestEvent;
import com.bit.idol.fanoutservice.service.FanoutService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// notify-request-topic을 받아서 fanout 처리 시작
@Component
@RequiredArgsConstructor
public class RequestConsumer {

    private final FanoutService fanoutService;
    private final ObjectMapper om;

    @KafkaListener(topics = "${fanout.topics.request}", groupId = "${fanout.consumer.group-id:fanout-request-group}")
    public void onRequest(String rawJson) {
        try {
            NotifyRequestEvent req = om.readValue(rawJson, NotifyRequestEvent.class);
            fanoutService.handle(req);
        } catch (Exception ignore) {
        }
    }
}
