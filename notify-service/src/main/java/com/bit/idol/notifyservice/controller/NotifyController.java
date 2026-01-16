package com.bit.idol.notifyservice.controller;

import com.bit.idol.notifyservice.dto.NotificationItemResponse;
import com.bit.idol.notifyservice.dto.NotificationListResponse;
import com.bit.idol.notifyservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notify")
public class NotifyController {

    private final NotificationService notificationService;

    public NotifyController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // 알림 목록 조회 (내 알림 = targetType=USER, targetId=userId 로 fanout이 풀어서 저장했다는 전제)
    @GetMapping("/notifications")
    public ResponseEntity<NotificationListResponse> listNotifications(
            @RequestHeader("X-User-Id") int userId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        return ResponseEntity.ok(notificationService.list(userId, cursor, size));
    }

    // 알림 단건 조회
    @GetMapping("/notifications/{id}")
    public ResponseEntity<NotificationItemResponse> getOne(
            @PathVariable("id") int notificationId
    ) {
        return ResponseEntity.ok(notificationService.getOne(notificationId));
    }
}
