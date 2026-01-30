package com.bit.idol.notifyservice.sse;

import com.bit.idol.notifyservice.dto.IdolMessageStackSsePayload;
import com.bit.idol.notifyservice.dto.NotificationSsePayload;
import com.bit.idol.notifyservice.entity.IdolMessageStack;
import com.bit.idol.notifyservice.entity.Notification;
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

    // 기존: 알림 1건 푸시
    public void pushToUser(int userId, Notification n) {
        NotificationSsePayload p = new NotificationSsePayload();
        p.setNotificationId(n.getNotificationId());
        p.setEventId(n.getEventId());
        p.setType(n.getType());
        p.setTargetType("USER");
        p.setTargetId(String.valueOf(n.getReceiverId()));
        p.setRedirectUrl(n.getRedirectUrl());
        p.setOccurredAt(n.getOccurredAt() != null ? n.getOccurredAt().toString() : null);
        p.setArgs(parseArgs(n.getArgsJson()));

        registry.send(userId, "notification", p);
    }

    // 신규: 스택 업데이트 푸시(서버가 계산한 정답)
    public void pushIdolMessageStackToUser(int userId, IdolMessageStack stack) {
        if (stack == null) return;

        IdolMessageStackSsePayload p = new IdolMessageStackSsePayload();
        p.setIdolId(stack.getIdolId());
        p.setUnreadCount(stack.getUnreadCount());
        p.setLastOccurredAt(stack.getLastOccurredAt() != null ? stack.getLastOccurredAt().toString() : null);

        registry.send(userId, "idol_message_stack", p);
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
