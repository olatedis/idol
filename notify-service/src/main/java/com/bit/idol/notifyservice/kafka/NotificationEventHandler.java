package com.bit.idol.notifyservice.kafka;

import com.bit.idol.notifyservice.entity.Notification;
import com.bit.idol.notifyservice.repository.NotificationRepository;
import com.bit.idol.notifyservice.service.IdolMessageStackService;
import com.bit.idol.notifyservice.service.PreferenceService; // 추가
import com.bit.idol.notifyservice.sse.IdolMessageStackSsePublisher;
import com.bit.idol.notifyservice.sse.NotificationSsePublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// kafka로 수신한 알림이벤트(json문자열)을 Notification 모델로 DB에 저장하는 핸들러
@Slf4j
@Component
public class NotificationEventHandler {

    private final ObjectMapper om;
    private final NotificationRepository notificationRepo;
    private final NotificationSsePublisher ssePublisher;

    // IDOL_MESSAGE 스택용
    private final IdolMessageStackService stackService;
    private final IdolMessageStackSsePublisher stackSsePublisher;

    private final PreferenceService preferenceService;

    public NotificationEventHandler(ObjectMapper om,
                                    NotificationRepository notificationRepo,
                                    NotificationSsePublisher ssePublisher,
                                    IdolMessageStackService stackService,
                                    IdolMessageStackSsePublisher stackSsePublisher,
                                    PreferenceService preferenceService) { // 수정
        this.om = om;
        this.notificationRepo = notificationRepo;
        this.ssePublisher = ssePublisher;
        this.stackService = stackService;
        this.stackSsePublisher = stackSsePublisher;
        this.preferenceService = preferenceService;
    }

    @Transactional
    public void handleNotification(String rawJson) {

        log.info("[NOTIFY][RECEIVED] rawJson={}", rawJson);

        try {
            JsonNode root = om.readTree(rawJson);

            String eventId = text(root, "eventId");
            String type = text(root, "type");
            String targetTypeStr = text(root, "targetType");
            String targetId = text(root, "targetId");
            String redirectUrl = text(root, "redirectUrl");
            String occurredAtStr = text(root, "occurredAt");
            JsonNode argsNode = root.get("args");

            if (blank(eventId) || blank(type) || blank(redirectUrl) || blank(occurredAtStr)) {
                log.warn("[NOTIFY][SKIP] 필수값 누락 eventId={}, type={}", eventId, type);
                return;
            }

            if (!"USER".equals(targetTypeStr)) {
                log.warn("[NOTIFY][SKIP] targetType != USER, targetType={}", targetTypeStr);
                return;
            }

            if (blank(targetId)) {
                log.warn("[NOTIFY][SKIP] targetId 없음");
                return;
            }

            int receiverId;
            try {
                receiverId = Integer.parseInt(targetId);
            } catch (Exception e) {
                log.warn("[NOTIFY][SKIP] targetId parse 실패 targetId={}", targetId);
                return;
            }

            if (!preferenceService.isEnabledForType(receiverId, type)) {
                log.warn("[NOTIFY][SKIP] preference 차단 receiverId={}, type={}", receiverId, type);
                return;
            }

            LocalDateTime occurredAt;
            try {
                occurredAt = LocalDateTime.parse(occurredAtStr);
            } catch (Exception e) {
                log.warn("[NOTIFY][SKIP] occurredAt parse 실패 value={}", occurredAtStr);
                return;
            }

            String argsJson = null;
            if (argsNode != null && !argsNode.isNull()) {
                argsJson = argsNode.toString();
            }

            if (notificationRepo.existsByEventId(eventId)) {
                log.warn("[NOTIFY][SKIP] 중복 eventId={}", eventId);
                return;
            }

            Notification n = Notification.create();
            n.setEventId(eventId);
            n.setReceiverId(receiverId);
            n.setType(type);
            n.setRedirectUrl(redirectUrl);
            n.setOccurredAt(occurredAt);
            n.setArgsJson(argsJson);

            try {
                Notification saved = notificationRepo.save(n);

                log.info("[NOTIFY][SAVED] eventId={}, receiverId={}, type={}",
                        saved.getEventId(),
                        saved.getReceiverId(),
                        saved.getType()
                );

                ssePublisher.pushToUser(saved.getReceiverId(), saved);

                if ("IDOL_MESSAGE".equals(saved.getType())) {
                    Long idolId = parseIdolIdFromRedirectUrl(saved.getRedirectUrl());
                    if (idolId != null) {
                        var stack = stackService.increase(saved.getReceiverId(), idolId, saved.getOccurredAt());
                        stackSsePublisher.pushToUser(saved.getReceiverId(), stack);
                    }
                }

            } catch (DataIntegrityViolationException dup) {
                log.warn("[NOTIFY][DUPLICATE] eventId={}", eventId);
            }

        } catch (Exception e) {
            log.error("[NOTIFY][ERROR] rawJson={}, error={}", rawJson, e.getMessage(), e);
        }
    }

    // redirectUrl: /chat/room/{idolId} 또는 /group/{groupId}/chat?idolId={idolId} 형태에서 idolId 파싱
    private static Long parseIdolIdFromRedirectUrl(String redirectUrl) {
        try {
            if (redirectUrl == null || redirectUrl.isBlank()) return null;

            // 1. 새로운 패턴: ?idolId=123 파싱
            if (redirectUrl.contains("idolId=")) {
                String term = "idolId=";
                int start = redirectUrl.indexOf(term) + term.length();
                int end = redirectUrl.indexOf("&", start);
                String val = (end == -1) ? redirectUrl.substring(start) : redirectUrl.substring(start, end);
                return Long.parseLong(val.trim());
            }

            // 2. 기존 패턴: /chat/room/123 파싱
            String prefix = "/chat/room/";
            int idx = redirectUrl.indexOf(prefix);
            if (idx >= 0) {
                String tail = redirectUrl.substring(idx + prefix.length());
                int q = tail.indexOf("?");
                if (q >= 0) tail = tail.substring(0, q);
                return Long.parseLong(tail.trim());
            }

            return null;
        } catch (Exception e) {
            return null;
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