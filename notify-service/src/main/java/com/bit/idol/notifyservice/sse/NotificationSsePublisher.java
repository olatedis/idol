package com.bit.idol.notifyservice.sse;

import com.bit.idol.notifyservice.entity.Notification;
import com.bit.idol.notifyservice.dto.NotificationSsePayload;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationSsePublisher {

    private final SseEmitterRegistry registry;
    private final ObjectMapper om;

    public void pushToUser(int userId, Notification n) {
        NotificationSsePayload p = new NotificationSsePayload();
        p.setNotificationId(n.getNotificationId());
        p.setEventId(n.getEventId());
        p.setType(n.getType());
        p.setTargetType(n.getTargetType().name());
        p.setTargetId(n.getTargetId());
        p.setRedirectUrl(n.getRedirectUrl());
        p.setOccurredAt(n.getOccurredAt() != null ? n.getOccurredAt().toString() : null);
        p.setArgs(parseArgs(n.getArgsJson()));

        registry.send(userId, "notification", p);
    }

    private Map<String, String> parseArgs(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return om.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
