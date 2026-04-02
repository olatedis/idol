package com.bit.paymentservice.api;

import com.bit.paymentservice.domain.dto.PaymentConfirmDto;
import com.bit.paymentservice.domain.dto.PaymentCreateRequest;
import com.bit.paymentservice.domain.dto.PaymentCreateResponse;
import com.bit.paymentservice.service.PaymentService;
import com.bit.paymentservice.domain.dto.*;
import com.bit.paymentservice.domain.entity.Payment;
import com.bit.paymentservice.infra.persistence.PaymentRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/payments")
@AllArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(
            @RequestHeader(value = "X-User-Id") int userId,
            @RequestBody PaymentConfirmDto dto) {
        try {
            log.info("결제 승인 컨트롤러 호출: orderId={}, userId={}", dto.getOrderId(), userId);

            // userId 검증
            if (userId <= 0) {
                log.error("유효하지 않은 사용자 ID: userId={}", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            paymentService.confirm(dto, userId);
            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            log.warn("결제 승인 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalStateException e) {
            log.warn("결제 승인 실패 - 상태 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            log.error("결제 승인 실패 - 서버 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/ready")
    public ResponseEntity<PaymentCreateResponse> createPayment(
            @RequestHeader("X-User-Id") int userId, // 헤더에서 ID 추출 (보안 강화)
            @RequestBody PaymentCreateRequest request) {
        try {
            // 요청 Body의 userId를 무시하고, 인증된 userId로 덮어씌움
            request.setUserId(userId);

            log.info("결제 준비 컨트롤러 호출: userId={}, domain={}, amount={}",
                    request.getUserId(), request.getDomain(), request.getAmount());

            PaymentCreateResponse response = paymentService.createPayment(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("결제 준비 실패 - 유효성 검사: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("결제 준비 실패 - 서버 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Payment> getPayment(@PathVariable String orderId) {
        try {
            log.info("결제 조회: orderId={}", orderId);
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("결제 정보가 없습니다."));
            return ResponseEntity.ok(payment);
        } catch (IllegalArgumentException e) {
            log.warn("결제 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<List<Payment>> getMyPayments(
            @RequestHeader("X-User-Id") int userId) {
        log.info("내 결제 내역 전체 목록 조회 컨트롤러 진입: userId={}", userId);
        return ResponseEntity.ok(paymentService.findMyPayments(userId));
    }

    /**
     * 결제 취소: READY 상태인 대기 결제를 삭제한다.
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deletePendingPayment(
            @RequestHeader("X-User-Id") int userId,
            @PathVariable String orderId) {
        try {
            // optional: could check user owns it
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("결제 정보가 없습니다."));
            if (payment.getUserId() != userId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            paymentService.deletePending(orderId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("결제 삭제 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            log.warn("결제 삭제 실패 - 상태 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("결제 삭제 실패 - 서버 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 빌링키 결제 완료 처리 (서비스 내부 전용, 외부 노출 X)
     * subscription-service가 빌링 결제 후 paymentKey를 전달해 READY → COMPLETED 전환
     */
    @PostMapping("/internal/billing-complete")
    public ResponseEntity<Void> billingComplete(@RequestBody BillingCompleteRequest req) {
        try {
            if (req.getOrderId() == null || req.getPaymentKey() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            paymentService.billingComplete(req.getOrderId(), req.getPaymentKey(), req.getAmount());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("빌링 완료 처리 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("빌링 완료 처리 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/agency/{agencyId}/revenue")
    public ResponseEntity<AgencyRevenueDto> getAgencyRevenue(
            @RequestHeader(value = "X-Role", required = false) String role,
            @PathVariable int agencyId) {
        if (!"ADMIN".equals(role) && !"AGENCY".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        long total = paymentRepository.sumAmountByAgencyIdAndStatusCompleted(agencyId,
                com.bit.paymentservice.domain.enumtype.PaymentStatus.COMPLETED);
        long concert = paymentRepository.sumAmountByAgencyIdAndStatusCompletedAndDomain(agencyId,
                com.bit.paymentservice.domain.enumtype.PaymentStatus.COMPLETED,
                com.bit.paymentservice.domain.enumtype.PaymentDomain.CONCERT);
        long subscription = paymentRepository.sumAmountByAgencyIdAndStatusCompletedAndDomain(agencyId,
                com.bit.paymentservice.domain.enumtype.PaymentStatus.COMPLETED,
                com.bit.paymentservice.domain.enumtype.PaymentDomain.SUBSCRIPTION);

        AgencyRevenueDto dto = new AgencyRevenueDto(agencyId, total, concert, subscription);
        return ResponseEntity.ok(dto);
    }
}
