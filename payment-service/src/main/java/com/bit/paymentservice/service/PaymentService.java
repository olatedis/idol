package com.bit.paymentservice.service;

import com.bit.paymentservice.domain.dto.*;
import com.bit.paymentservice.domain.entity.Payment;
import com.bit.paymentservice.infra.persistence.PaymentRepository;
import com.bit.paymentservice.infra.toss.TossPgClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPgClient tossPgClient;
    private final PaymentTransactionService paymentTransactionService;

    @Transactional
    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {
        // 사용자 ID 검증
        if (request.getUserId() <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID");
        }

        // 금액 검증
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }

        // targetId 검증
        if (request.getTargetId() <= 0) {
            throw new IllegalArgumentException("유효하지 않은 대상 ID");
        }

        String orderId = UUID.randomUUID().toString();

        Payment payment = Payment.ready(
                orderId,
                request.getAmount(),
                request.getDomain(),
                request.getTargetId(),
                request.getUserId(),
                request.getReservationIds());

        paymentRepository.save(payment);
        log.info("결제 준비 완료: orderId={}, userId={}, amount={}, domain={}, reservationIds={}",
                orderId, request.getUserId(), request.getAmount(), request.getDomain(), request.getReservationIds());

        return new PaymentCreateResponse(orderId, payment.getAmount());
    }

    // 트랜잭션 분리: 검증(DB) -> 외부호출(No TX) -> 업데이트(DB)
    public void confirm(PaymentConfirmDto dto, int requestUserId) {
        log.info("결제 승인 요청: orderId={}, requestUserId={}", dto.getOrderId(), requestUserId);

        // 1. 검증 (읽기 전용 트랜잭션)
        Payment payment = paymentTransactionService.validatePayment(dto, requestUserId);

        // 2. 외부 API 호출 (트랜잭션 없음 - DB 커넥션 점유 안 함)
        TossConfirmResponse response;
        try {
            log.info("토스페이먼츠 승인 API 호출: orderId={}, amount={}", dto.getOrderId(), dto.getAmount());
            response = tossPgClient.confirm(new TossConfirmRequest(
                    dto.getPaymentKey(),
                    dto.getOrderId(),
                    dto.getAmount()));
        } catch (Exception e) {
            // 실패 시 상태 업데이트 (별도 트랜잭션)
            paymentTransactionService.markAsFailed((long) payment.getId());
            log.error("토스페이먼츠 API 호출 실패: orderId={}, error={}", dto.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("결제 승인 중 오류가 발생했습니다: " + e.getMessage(), e);
        }

        // 3. 성공 처리 (짧은 트랜잭션)
        if ("DONE".equals(response.getStatus())) {
            paymentTransactionService.completePayment((long) payment.getId(), response.getPaymentKey());
        } else {
            paymentTransactionService.markAsFailed((long) payment.getId());
            log.warn("토스페이먼츠 결제 거절: orderId={}, tossStatus={}", dto.getOrderId(), response.getStatus());
            throw new IllegalStateException("결제가 거절되었습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<Payment> findMyPayments(int userId) {
        log.info("결제 내역 조회: userId={}", userId);
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
