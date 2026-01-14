package com.bit.idol.notifyservice.service;

import com.bit.idol.notifyservice.dto.*;
import com.bit.idol.notifyservice.entity.Notification;
import com.bit.idol.notifyservice.entity.NotificationType;
import com.bit.idol.notifyservice.repository.NotificationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class NotificationService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final NotificationRepository repo;
    private final ObjectMapper om;

    public NotificationService(NotificationRepository repo, ObjectMapper om) {
        this.repo = repo;
        this.om = om;
    }

    @Transactional(readOnly = true)
    public NotificationListResponse list(int userId, String cursorIso, Integer size, String type, Boolean unreadOnly) {
        int s = (size == null) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        boolean unread = (unreadOnly != null) && unreadOnly;

        NotificationType category = null;
        if (type != null && !type.isBlank()) category = NotificationType.valueOf(type);

        LocalDateTime cursor = null;
        if (cursorIso != null && !cursorIso.isBlank()) cursor = LocalDateTime.parse(cursorIso, ISO);

        var pageable = PageRequest.of(0, s);
        List<Notification> list = repo.findListByCursor(userId, category, unread, cursor, pageable);

        NotificationListResponse res = new NotificationListResponse();
        res.items = new ArrayList<>();
        for (Notification n : list) res.items.add(toResponse(n));

        res.nextCursor = list.isEmpty() ? null : list.get(list.size() - 1).getCreatedAt().format(ISO);
        res.hasNext = list.size() == s;
        return res;
    }

    // 읽지않은 알림개수 조회
    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(int userId) {
        return new UnreadCountResponse(repo.countByReceiverIdAndReadAtIsNull(userId));
    }

    // 특정알림 읽음처리
    @Transactional
    public MarkReadResponse markRead(int userId, int notificationId) {
        Notification n = repo.findById(notificationId).orElseThrow(EntityNotFoundException::new);
        if (n.getReceiverId() != userId) throw new SecurityException("FORBIDDEN");

        if (n.getReadAt() == null) {
            n.setReadAt(LocalDateTime.now());
            repo.save(n);
        }

        MarkReadResponse res = new MarkReadResponse();
        res.notificationId = n.getNotificationId();
        res.readAt = (n.getReadAt() == null) ? null : n.getReadAt().format(ISO);
        return res;
    }

    // 전체읽음처리
    @Transactional
    public MarkAllReadResponse markAllRead(int userId) {
        LocalDateTime now = LocalDateTime.now();
        int updated = repo.markAllRead(userId, now);

        MarkAllReadResponse res = new MarkAllReadResponse();
        res.updatedCount = updated;
        res.readAt = now.format(ISO);
        return res;
    }

    private NotificationItemResponse toResponse(Notification n) {
        NotificationItemResponse dto = new NotificationItemResponse();
        dto.notificationId = n.getNotificationId();
        dto.category = n.getCategory().name();
        dto.eventType = n.getEventType();

        dto.title = n.getTitle();
        dto.body = n.getBody();
        dto.deeplink = n.getDeeplink();

        dto.refType = n.getRefType().name();
        dto.refId = n.getRefId();

        dto.attributes = parseAttributes(n.getAttributesJson());

        dto.createdAt = n.getCreatedAt().format(ISO);
        dto.readAt = (n.getReadAt() == null) ? null : n.getReadAt().format(ISO);
        return dto;
    }

    private Map<String, Object> parseAttributes(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return om.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
