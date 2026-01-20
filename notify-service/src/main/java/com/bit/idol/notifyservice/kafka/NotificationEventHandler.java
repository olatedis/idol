package com.bit.idol.notifyservice.kafka;

import com.bit.idol.notifyservice.entity.Notification;
import com.bit.idol.notifyservice.entity.TargetType;
import com.bit.idol.notifyservice.repository.NotificationRepository;
import com.bit.idol.notifyservice.sse.NotificationSsePublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// kafka로 수신한 알림이벤트(json문자열)을 새 Notification 모델로 DB에 저장하는 핸들러
@Component
public class NotificationEventHandler {

    private final ObjectMapper om;
    private final NotificationRepository notificationRepo;
    private final NotificationSsePublisher ssePublisher;

    public NotificationEventHandler(ObjectMapper om,
                                    NotificationRepository notificationRepo,
                                    NotificationSsePublisher ssePublisher) {
        this.om = om;
        this.notificationRepo = notificationRepo;
        this.ssePublisher = ssePublisher;
    }

    @Transactional
    public void handleNotification(String rawJson) {
        try {
            JsonNode root = om.readTree(rawJson);

            // 필수 필드 파싱
            String eventId = text(root, "eventId");   // uuid
            String type = text(root, "type");   // 알림 종류
            String targetTypeStr = text(root, "targetType");   // USER/ALL/IDOL_SUB/GROUP_SUB
            String targetId = text(root, "targetId");   // USER면 userId, ALL이면 null 가능
            String redirectUrl = text(root, "redirectUrl");   // 클릭 이동 링크
            String occurredAtStr = text(root, "occurredAt");   // ISO 문자열
            JsonNode argsNode = root.get("args");   // Map<String,String>

            if (blank(eventId) || blank(type) || blank(targetTypeStr) || blank(redirectUrl) || blank(occurredAtStr)) {
                return;
            }

            TargetType targetType;
            try {
                targetType = TargetType.valueOf(targetTypeStr);
            } catch (Exception e) {
                return;
            }

            // targetType=ALL이면 targetId는 null 허용, 그 외는 targetId가 필요
            if (targetType != TargetType.ALL && blank(targetId)) {
                return;
            }

            LocalDateTime occurredAt;
            try {
                occurredAt = LocalDateTime.parse(occurredAtStr); // 기본 ISO_LOCAL_DATE_TIME 형태 기대
            } catch (Exception e) {
                return;
            }

            // args는 없을 수도 있음(null 허용). 있으면 JSON 문자열로 저장
            String argsJson = null;
            if (argsNode != null && !argsNode.isNull()) {
                argsJson = argsNode.toString();
            }

            // eventId 중복 방지(이미 저장된 이벤트면 무시)
            if (notificationRepo.existsByEventId(eventId)) {
                return;
            }

            Notification n = Notification.create();
            n.setEventId(eventId);
            n.setType(type);
            n.setTargetType(targetType);
            n.setTargetId(targetId);
            n.setRedirectUrl(redirectUrl);
            n.setOccurredAt(occurredAt);
            n.setArgsJson(argsJson);

            try {
                Notification saved = notificationRepo.save(n);

                if (saved.getTargetType() == TargetType.USER && !blank(saved.getTargetId())) {
                    int userId = Integer.parseInt(saved.getTargetId());
                    ssePublisher.pushToUser(userId, saved);
                }

            } catch (DataIntegrityViolationException dup) {
            }

        } catch (Exception ignore) {
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
