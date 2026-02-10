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

@RestController
@RequestMapping("/payments")
@AllArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(
            @RequestHeader(value = "X-User-Id", required = true) int userId,
            @RequestBody PaymentConfirmDto dto
    ) {
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
            @RequestBody PaymentCreateRequest request
    ) {
        try {
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
}
