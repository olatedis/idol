package com.bit.subscriptionservice.controller;

import com.bit.subscriptionservice.dto.*;
import com.bit.subscriptionservice.dto.*;
import com.bit.subscriptionservice.entity.BillingKey;
import com.bit.subscriptionservice.entity.Role;
import com.bit.subscriptionservice.service.BillingKeyService;
import com.bit.subscriptionservice.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final BillingKeyService billingKeyService;

    // 개인(아이돌) 구독하기
    @PostMapping
    public ResponseEntity<SubscriptionDto> subscribe(
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
            @Valid @RequestBody SubscriptionCreateRequest request) {
        if (Role.valueOf(role) == Role.AGENCY) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.subscribe(userId, request));
    }
    /**
     * 디버그/결제취소용: PENDING 상태인 구독을 삭제한다.
     */
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<Void> deletePendingSubscription(
            @RequestHeader("X-User-Id") int userId,
            @PathVariable int subscriptionId
    ) {
        try {
            subscriptionService.deletePending(userId, subscriptionId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    // 개인(아이돌) 구독 해지
    @PostMapping("/cancel")
    public ResponseEntity<Void> cancel(
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
            @Valid @RequestBody SubscriptionCancelRequest request) {
        if (Role.valueOf(role) == Role.AGENCY) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        subscriptionService.cancel(userId, request);
        return ResponseEntity.ok().build();
    }

    // 내 개인(아이돌) 구독 목록
    @GetMapping("/me")
    public ResponseEntity<List<SubscriptionDto>> getMySubscriptions(
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role) {
        if (Role.valueOf(role) == Role.AGENCY) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(subscriptionService.getMySubscriptions(userId));
    }

    // 아이돌 구독자 수 조회
    @GetMapping("/count/{idolId}")
    public ResponseEntity<Integer> getSubscriptionCount(@PathVariable int idolId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionCount(idolId));
    }

    // 내부용: 개인(아이돌) 구독 여부 확인(채팅 서비스용)
    @GetMapping("/internal/check")
    public ResponseEntity<Boolean> checkSubscription(
            @RequestHeader("X-User-Id") int userId,
            @RequestParam int idolId) {
        return ResponseEntity.ok(subscriptionService.isSubscribed(userId, idolId));
    }

    // 그룹 구독하기
    @PostMapping("/groups")
    public ResponseEntity<GroupSubscriptionDto> subscribeGroup(
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
            @Valid @RequestBody GroupSubscriptionCreateRequest request) {
        if (Role.valueOf(role) == Role.AGENCY) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.subscribeGroup(userId, request));
    }

    // 그룹 구독 해지
    @PostMapping("/groups/cancel")
    public ResponseEntity<Void> cancelGroup(
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
            @Valid @RequestBody GroupSubscriptionCancelRequest request) {
        if (Role.valueOf(role) == Role.AGENCY) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        subscriptionService.cancelGroup(userId, request);
        return ResponseEntity.ok().build();
    }

    // 내 그룹 구독 목록
    @GetMapping("/groups/me")
    public ResponseEntity<List<GroupSubscriptionDto>> getMyGroupSubscriptions(
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role) {
        if (Role.valueOf(role) == Role.AGENCY) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(subscriptionService.getMyGroupSubscriptions(userId));
    }

    // ===== 빌링키 관련 엔드포인트 =====

    /**
     * 빌링키 발급 요청
     * POST /subscriptions/billing/authorize
     * Body: { "idolId": 1, "authKey": "auth_xxx", "plan": "MONTHLY" }
     */
    @PostMapping("/billing/authorize")
    public ResponseEntity<BillingKeyAuthResponse> authorizeBillingKey(
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
            @Valid @RequestBody BillingKeyAuthRequest request) {
        if (Role.valueOf(role) == Role.AGENCY) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            String customerKey = UUID.randomUUID().toString();
            BillingKey billingKey = billingKeyService.issueBillingKey(
                    request.getAuthKey(),
                    userId,
                    request.getIdolId(),
                    customerKey);

            log.info("빌링키 발급 성공: userId={}, idolId={}, plan={}",
                    userId, request.getIdolId(), request.getPlan());

            // 정기 결제으로 처리되는 경우, 이미 PENDING 상태로 생성된 구독이 있을 수 있음
            try {
                subscriptionService.activatePendingSubscription(userId, request.getIdolId());
            } catch (Exception e) {
                log.warn("빌링키 발급 후 구독 활성화 실패: {}", e.getMessage());
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new BillingKeyAuthResponse(
                            billingKey.getId(),
                            billingKey.getCardNumber(),
                            billingKey.getCardIssuer(),
                            billingKey.getCardType(),
                            "빌링키가 등록되었습니다. 이제 자동 구독이 활성화됩니다."));
        } catch (Exception e) {
            log.error("빌링키 발급 실패: userId={}, idolId={}, error={}",
                    userId, request.getIdolId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(new BillingKeyAuthResponse(
                            0,
                            null,
                            null,
                            null,
                            "빌링키 발급 실패: " + e.getMessage()));
        }
    }

    /**
     * 빌링키 존재 여부 확인
     * GET /subscriptions/billing/{idolId}
     */
    @GetMapping("/billing/{idolId}")
    public ResponseEntity<BillingKeyCheckResponse> checkBillingKey(
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
            @PathVariable int idolId) {
        if (Role.valueOf(role) == Role.AGENCY) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean hasKey = billingKeyService.hasBillingKey(userId, idolId);
        return ResponseEntity.ok(new BillingKeyCheckResponse(hasKey));
    }

    /**
     * 빌링키 삭제 (구독 취소)
     * DELETE /subscriptions/billing/{idolId}
     */
    @DeleteMapping("/billing/{idolId}")
    public ResponseEntity<Void> deleteBillingKey(
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
            @PathVariable int idolId) {
        if (Role.valueOf(role) == Role.AGENCY) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            billingKeyService.deleteBillingKey(userId, idolId);
            log.info("빌링키 삭제 성공: userId={}, idolId={}", userId, idolId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("빌링키 삭제 실패: userId={}, idolId={}, error={}",
                    userId, idolId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
