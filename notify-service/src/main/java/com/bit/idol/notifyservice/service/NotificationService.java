package com.bit.idol.notifyservice.service;

import com.bit.idol.notifyservice.dto.NotificationItemResponse;
import com.bit.idol.notifyservice.dto.NotificationListResponse;
import com.bit.idol.notifyservice.entity.Notification;
import com.bit.idol.notifyservice.repository.NotificationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // 내 알림 조회: notify DB는 receiverId 기준으로 USER 알림을 저장한다는 전제
    @Transactional(readOnly = true)
    public NotificationListResponse list(int userId, String cursorIso, Integer size) {
        int s = (size == null) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        LocalDateTime cursor = null;
        if (cursorIso != null && !cursorIso.isBlank()) {
            cursor = LocalDateTime.parse(cursorIso, ISO);
        }

        var pageable = PageRequest.of(0, s);
        List<Notification> list = repo.findListByCursor(userId, cursor, pageable);

        NotificationListResponse res = new NotificationListResponse();
        res.items = new ArrayList<>();
        for (Notification n : list) {
            res.items.add(toResponse(n));
        }

        res.nextCursor = list.isEmpty() ? null : list.get(list.size() - 1).getOccurredAt().format(ISO);
        res.hasNext = list.size() == s;
        return res;
    }

    // 특정 알림 단건 조회(필요하면)
    // // 타인 알림 조회 방지: receiverId가 요청자(userId)인지 검증
    @Transactional(readOnly = true)
    public NotificationItemResponse getOne(int userId, int notificationId) {
        Notification n = repo.findById(notificationId).orElseThrow(EntityNotFoundException::new);

        // // 소유자 검증(내 알림만 조회 가능)
        if (n.getReceiverId() != userId) {
            throw new SecurityException("FORBIDDEN");
        }

        return toResponse(n);
    }

    private NotificationItemResponse toResponse(Notification n) {
        NotificationItemResponse dto = new NotificationItemResponse();
        dto.notificationId = n.getNotificationId();
        dto.eventId = n.getEventId();
        dto.type = n.getType();

        // // 저장 모델은 receiverId 기반이므로 응답은 USER로 고정
        dto.targetType = "USER";
        dto.targetId = String.valueOf(n.getReceiverId());

        dto.redirectUrl = n.getRedirectUrl();
        dto.occurredAt = n.getOccurredAt().format(ISO);

        dto.args = parseArgs(n.getArgsJson());
        return dto;
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
