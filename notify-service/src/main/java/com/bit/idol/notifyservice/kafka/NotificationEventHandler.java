// src/main/java/com/bit/idol/notifyservice/kafka/NotificationEventHandler.java
package com.bit.idol.notifyservice.kafka;

import com.bit.idol.notifyservice.entity.Notification;
import com.bit.idol.notifyservice.repository.NotificationRepository;
import com.bit.idol.notifyservice.service.IdolMessageStackService;
import com.bit.idol.notifyservice.sse.IdolMessageStackSsePublisher;
import com.bit.idol.notifyservice.sse.NotificationSsePublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// kafka로 수신한 알림이벤트(json문자열)을 Notification 모델로 DB에 저장하는 핸들러
@Component
public class NotificationEventHandler {

    private final ObjectMapper om;
    private final NotificationRepository notificationRepo;
    private final NotificationSsePublisher ssePublisher;

    // IDOL_MESSAGE 스택용
    private final IdolMessageStackService stackService;
    private final IdolMessageStackSsePublisher stackSsePublisher;

    public NotificationEventHandler(ObjectMapper om,
                                    NotificationRepository notificationRepo,
                                    NotificationSsePublisher ssePublisher,
                                    IdolMessageStackService stackService,
                                    IdolMessageStackSsePublisher stackSsePublisher) {
        this.om = om;
        this.notificationRepo = notificationRepo;
        this.ssePublisher = ssePublisher;
        this.stackService = stackService;
        this.stackSsePublisher = stackSsePublisher;
    }

    @Transactional
    public void handleNotification(String rawJson) {
        try {
            JsonNode root = om.readTree(rawJson);

            // 필수 필드 파싱 (fanout 이벤트는 USER 단위로 들어온다는 전제)
            String eventId = text(root, "eventId");          // "원본UUID:userId" 형태
            String type = text(root, "type");               // 알림 종류
            String targetTypeStr = text(root, "targetType");// 기대값: "USER"
            String targetId = text(root, "targetId");       // userId
            String redirectUrl = text(root, "redirectUrl"); // 클릭 이동 링크
            String occurredAtStr = text(root, "occurredAt");// ISO 문자열
            JsonNode argsNode = root.get("args");           // Map<String,String>

            if (blank(eventId) || blank(type) || blank(redirectUrl) || blank(occurredAtStr)) {
                return;
            }

            // fanout-topic에서는 USER만 온다. 아니면 무시.
            if (!"USER".equals(targetTypeStr)) {
                return;
            }

            // USER면 targetId(userId)는 필수
            if (blank(targetId)) {
                return;
            }

            int receiverId;
            try {
                receiverId = Integer.parseInt(targetId);
            } catch (Exception e) {
                return;
            }

            LocalDateTime occurredAt;
            try {
                occurredAt = LocalDateTime.parse(occurredAtStr); // ISO_LOCAL_DATE_TIME 기대
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
            n.setReceiverId(receiverId);
            n.setType(type);
            n.setRedirectUrl(redirectUrl);
            n.setOccurredAt(occurredAt);
            n.setArgsJson(argsJson);

            try {
                Notification saved = notificationRepo.save(n);

                // 1) 저장 성공 시 Notification SSE 푸시
                ssePublisher.pushToUser(saved.getReceiverId(), saved);

                // 2) IDOL_MESSAGE면 스택 +1 처리 + 스택 SSE 푸시
                if ("IDOL_MESSAGE".equals(saved.getType())) {
                    Long idolId = parseIdolIdFromRedirectUrl(saved.getRedirectUrl());
                    if (idolId != null) {
                        var stack = stackService.increase(saved.getReceiverId(), idolId, saved.getOccurredAt());
                        stackSsePublisher.pushToUser(saved.getReceiverId(), stack);
                    }
                }

            } catch (DataIntegrityViolationException dup) {
                // 유니크(event_id) 충돌이면 그냥 무시(중복 처리)
            }

        } catch (Exception ignore) {
        }
    }

    // redirectUrl: /chat/room/{idolId} 형태에서 idolId 파싱
    private static Long parseIdolIdFromRedirectUrl(String redirectUrl) {
        try {
            if (redirectUrl == null) return null;

            // 기대 패턴: /chat/room/123
            String prefix = "/chat/room/";
            int idx = redirectUrl.indexOf(prefix);
            if (idx < 0) return null;

            String tail = redirectUrl.substring(idx + prefix.length());
            if (tail.isBlank()) return null;

            // 혹시 뒤에 쿼리스트링 붙는 경우 대비
            int q = tail.indexOf("?");
            if (q >= 0) tail = tail.substring(0, q);

            return Long.parseLong(tail.trim());
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
