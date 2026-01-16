package com.bit.idol.notifyservice.kafka;

import com.bit.idol.notifyservice.entity.Notification;
import com.bit.idol.notifyservice.entity.NotificationType;
import com.bit.idol.notifyservice.repository.NotificationPreferenceRepository;
import com.bit.idol.notifyservice.repository.NotificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// kafka로 수신한 알림이벤트(json문자열)을 실제 db에 저장하는 핸들러
@Component
public class NotificationEventHandler {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ObjectMapper om;
    private final NotificationRepository notificationRepo; // 알림 저장용
    private final NotificationPreferenceRepository prefRepo; // 알림설정 조회용

    public NotificationEventHandler(ObjectMapper om,
                                    NotificationRepository notificationRepo,
                                    NotificationPreferenceRepository prefRepo) {
        this.om = om;
        this.notificationRepo = notificationRepo;
        this.prefRepo = prefRepo;
    }

    // kafka로부터 전달받은 알림이벤트를 처리하는 메인 메서드
    @Transactional
    public void handleNotification(String rawJson) {
        try {
            JsonNode root = om.readTree(rawJson);

            String eventId = text(root, "eventId");
            String eventType = text(root, "eventType");
            String occurredAt = text(root, "occurredAt");
            JsonNode data = root.path("data");

            // 필수필드 없으면 이벤트 처리안하고 종료
            if (blank(eventId) || blank(eventType) || data.isMissingNode()) return;

            // 수신자 정보 파싱 (fanout 이후에는 항상 단일 유저)
            int receiverId = data.path("receiverId").asInt(-1);
            if (receiverId <= 0) return;

            // 알림 분류(category)는 필수로 유지
            NotificationType category;
            try {
                category = NotificationType.valueOf(text(data, "category"));
            } catch (Exception e) {
                return; // category 없거나 값이 이상하면 저장하지 않음
            }

            // 유저 알림설정에 따라 차단되는지
            if (!isAllowed(receiverId, category)) return;

            Notification n = Notification.create();
            n.setReceiverId(receiverId);
            n.setCategory(category);
            n.setEventId(eventId);
            n.setEventType(eventType);

            n.setTitle(text(data, "title"));
            n.setBody(text(data, "body"));
            n.setDeeplink(text(data, "deeplink"));

            // ✅ refType/refId 완전 제거 (파싱/검증/저장 안함)

            // attributes 처리 - 서비스마다 다르므로 json그대로 문자열로 저장
            JsonNode attrs = data.get("attributes");
            if (attrs != null && !attrs.isNull()) {
                n.setAttributesJson(attrs.toString());
            }

            n.setCreatedAt(parseTimeOrNull(occurredAt));

            try {
                notificationRepo.save(n);
            } catch (DataIntegrityViolationException dup) {
                // UNIQUE(receiver_id,event_id)로 중복 안되게
            }
        } catch (Exception ignore) {
        }
    }

    // 유저 알림설정에 따라 알림 허용되는지
    private boolean isAllowed(int userId, NotificationType category) {
        if (category == NotificationType.SYSTEM) return true;
        var opt = prefRepo.findById(userId);
        if (opt.isEmpty()) return true;

        var p = opt.get();
        return switch (category) {
            case CHAT -> p.isChatEnabled();
            case VOTE -> p.isVoteEnabled();
            case TICKET -> p.isTicketEnabled();
            case NOTICE -> p.isNoticeEnabled();
            case SYSTEM -> true;
        };
    }

    // ISO 형식을 localDateTime으로 변환
    private LocalDateTime parseTimeOrNull(String iso) {
        try {
            if (blank(iso)) return null;
            return LocalDateTime.parse(iso, ISO);
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
