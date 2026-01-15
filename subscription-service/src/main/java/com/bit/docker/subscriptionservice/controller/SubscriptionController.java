package com.bit.docker.subscriptionservice.controller;

import com.bit.docker.subscriptionservice.dto.SubscriptionCancelRequest;
import com.bit.docker.subscriptionservice.dto.SubscriptionCreateRequest;
import com.bit.docker.subscriptionservice.dto.SubscriptionDto;
import com.bit.docker.subscriptionservice.entity.Role;
import com.bit.docker.subscriptionservice.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /* =========================
       구독하기
     ========================= */
    @PostMapping
    public ResponseEntity<SubscriptionDto> subscribe(
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
            @Valid @RequestBody SubscriptionCreateRequest request
    ) {
        if (Role.valueOf(role) != Role.USER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.subscribe(userId, request));
    }

    /* =========================
       구독 해지
     ========================= */
    @PostMapping("/cancel")
    public ResponseEntity<Void> cancel(
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
            @Valid @RequestBody SubscriptionCancelRequest request
    ) {
        if (Role.valueOf(role) != Role.USER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        subscriptionService.cancel(userId, request);
        return ResponseEntity.ok().build();
    }

    /* =========================
       내 구독 목록
     ========================= */
    @GetMapping("/me")
    public ResponseEntity<List<SubscriptionDto>> getMySubscriptions(
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role
    ) {
        if (Role.valueOf(role) != Role.USER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(subscriptionService.getMySubscriptions(userId));
    }

    /* =========================
       내부용: 구독 여부 확인
       (채팅 서비스에서 사용)
     ========================= */
    @GetMapping("/internal/check")
    public ResponseEntity<Boolean> checkSubscription(
            @RequestHeader("X-User-Id") int userId,
            @RequestParam Long idolId
    ) {
        return ResponseEntity.ok(
                subscriptionService.isSubscribed(userId, idolId));
    }
}

