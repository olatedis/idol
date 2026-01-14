package com.bit.idol.notifyservice.controller;

import com.bit.idol.notifyservice.dto.*;
import com.bit.idol.notifyservice.service.NotificationService;
import com.bit.idol.notifyservice.service.PreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notify")
public class NotifyController {

    private final PreferenceService preferenceService;
    private final NotificationService notificationService;

    public NotifyController(PreferenceService preferenceService, NotificationService notificationService) {
        this.preferenceService = preferenceService;
        this.notificationService = notificationService;
    }

    // 유저 설정 조회 (알림 on/off 설정)
    @GetMapping("/preferences")
    public ResponseEntity<PreferenceResponse> getPreferences(@RequestHeader("X-User-Id") int userId) {
        return ResponseEntity.ok(preferenceService.getOrCreate(userId));
    }

    @PutMapping("/preferences")
    public ResponseEntity<PreferenceResponse> updatePreferences(
            @RequestHeader("X-User-Id") int userId,
            @RequestBody UpdatePreferenceRequest req
    ) {
        return ResponseEntity.ok(preferenceService.update(userId, req));
    }

    // 알림 목록 조회
    @GetMapping("/notifications")
    public ResponseEntity<NotificationListResponse> listNotifications(
            @RequestHeader("X-User-Id") int userId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly
    ) {
        return ResponseEntity.ok(notificationService.list(userId, cursor, size, type, unreadOnly));
    }

    // 읽지않은 알림개수 조회
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount(@RequestHeader("X-User-Id") int userId) {
        return ResponseEntity.ok(notificationService.unreadCount(userId));
    }

    // 특정알림 읽음처리
    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<MarkReadResponse> markRead(
            @RequestHeader("X-User-Id") int userId,
            @PathVariable("id") int notificationId
    ) {
        return ResponseEntity.ok(notificationService.markRead(userId, notificationId));
    }

    // 전체읽음처리
    @PatchMapping("/notifications/read-all")
    public ResponseEntity<MarkAllReadResponse> markAllRead(@RequestHeader("X-User-Id") int userId) {
        return ResponseEntity.ok(notificationService.markAllRead(userId));
    }
}
