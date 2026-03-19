package com.bit.idol.fanoutservice.kafka;

import com.bit.idol.fanoutservice.dto.NotifyRequestEvent;
import com.bit.idol.fanoutservice.service.FanoutService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// notify-request-topic을 받아서 fanout 처리 시작
@Component
@RequiredArgsConstructor
@Slf4j
public class RequestConsumer {

    private final FanoutService fanoutService;
    private final ObjectMapper om;

    @KafkaListener(topics = "${fanout.topics.request}", groupId = "${fanout.consumer.group-id:fanout-request-group}")
    public void onRequest(String rawJson) {

        log.info("[FANOUT][RECEIVED] rawJson={}", rawJson);

        NotifyRequestEvent req;
        try {
            String normalizedJson = rawJson;

            // 수정:
            // Kafka에 JSON 문자열이 "문자열 형태로 한 번 더" 들어온 경우 벗겨냄
            if (normalizedJson != null
                    && normalizedJson.length() >= 2
                    && normalizedJson.startsWith("\"")
                    && normalizedJson.endsWith("\"")) {

                normalizedJson = om.readValue(normalizedJson, String.class);
                log.warn("[FANOUT][UNWRAP] double-serialized payload detected. normalizedJson={}", normalizedJson);
            }

            req = om.readValue(normalizedJson, NotifyRequestEvent.class);

        } catch (Exception e) {
            log.error("[FANOUT][PARSE ERROR] rawJson={}, error={}", rawJson, e.getMessage(), e);
            return;
        }

        log.info("[FANOUT][PARSE SUCCESS] eventId={}, type={}, targetType={}, targetId={}",
                req.getEventId(),
                req.getType(),
                req.getTargetType(),
                req.getTargetId()
        );

        try {
            fanoutService.handle(req);
            log.info("[FANOUT][HANDLE SUCCESS] eventId={}", req.getEventId());
        } catch (Exception e) {
            log.error("[FANOUT][HANDLE ERROR] eventId={}, error={}",
                    req.getEventId(), e.getMessage(), e);
        }
    }
}