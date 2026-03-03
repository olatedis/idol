package com.bit.paymentservice.service;

import com.bit.paymentservice.domain.dto.*;
import com.bit.paymentservice.domain.dto.PaymentConfirmDto;
import com.bit.paymentservice.domain.dto.PaymentCreateRequest;
import com.bit.paymentservice.domain.dto.PaymentCreateResponse;
import com.bit.paymentservice.domain.dto.TossConfirmRequest;
import com.bit.paymentservice.domain.dto.TossConfirmResponse;
import com.bit.paymentservice.domain.entity.Payment;
import com.bit.paymentservice.domain.enumtype.PaymentStatus;
import com.bit.paymentservice.domain.event.PaymentCompletedEvent;
import com.bit.paymentservice.infra.persistence.PaymentRepository;
import com.bit.paymentservice.infra.toss.TossPgClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPgClient tossPgClient;
    private final ApplicationEventPublisher eventPublisher;

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
                request.getUserId());

        paymentRepository.save(payment);
        log.info("결제 준비 완료: orderId={}, userId={}, amount={}, domain={}",
                orderId, request.getUserId(), request.getAmount(), request.getDomain());

        return new PaymentCreateResponse(orderId, payment.getAmount());
    }

    // 트랜잭션 분리: 검증(DB) -> 외부호출(No TX) -> 업데이트(DB)
    public void confirm(PaymentConfirmDto dto, int requestUserId) {
        log.info("결제 승인 요청: orderId={}, requestUserId={}", dto.getOrderId(), requestUserId);

        // 1. 검증 (읽기 전용 트랜잭션)
        Payment payment = validatePayment(dto, requestUserId);

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
            markAsFailed((long) payment.getId());
            log.error("토스페이먼츠 API 호출 실패: orderId={}, error={}", dto.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("결제 승인 중 오류가 발생했습니다: " + e.getMessage(), e);
        }

        // 3. 성공 처리 (짧은 트랜잭션)
        if ("DONE".equals(response.getStatus())) {
            completePayment((long) payment.getId(), response.getPaymentKey());
        } else {
            markAsFailed((long) payment.getId());
            log.warn("토스페이먼츠 결제 거절: orderId={}, tossStatus={}", dto.getOrderId(), response.getStatus());
            throw new IllegalStateException("결제가 거절되었습니다.");
        }
    }

    @Transactional(readOnly = true)
    public Payment validatePayment(PaymentConfirmDto dto, int requestUserId) {
        Payment payment = paymentRepository.findByOrderId(dto.getOrderId())
                .orElseThrow(() -> {
                    log.error("주문 없음: orderId={}", dto.getOrderId());
                    return new IllegalArgumentException("주문 없음");
                });

        // 사용자 검증
        if (payment.getUserId() != requestUserId) {
            log.warn("권한 없는 결제 승인 시도: orderId={}, paymentUserId={}, requestUserId={}",
                    dto.getOrderId(), payment.getUserId(), requestUserId);
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        // 상태 검증
        if (payment.getStatus() != PaymentStatus.READY) {
            log.warn("이미 처리된 주문: orderId={}, status={}", dto.getOrderId(), payment.getStatus());
            throw new IllegalStateException("이미 처리된 주문");
        }

        // 금액 검증
        if (payment.getAmount() != dto.getAmount()) {
            log.error("금액 불일치: orderId={}, paymentAmount={}, requestAmount={}",
                    dto.getOrderId(), payment.getAmount(), dto.getAmount());
            throw new IllegalArgumentException("금액 불일치");
        }

        // PaymentKey 검증
        if (dto.getPaymentKey() == null || dto.getPaymentKey().isEmpty()) {
            log.error("결제 키 없음: orderId={}", dto.getOrderId());
            throw new IllegalArgumentException("결제 키가 필요합니다.");
        }

        return payment;
    }

    @Transactional
    public void completePayment(Long paymentId, String paymentKey) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보가 없습니다."));

        payment.complete(paymentKey, payment.getAmount());
        log.info("결제 승인 완료(DB): orderId={}, paymentKey={}", payment.getOrderId(), paymentKey);

        // 이벤트 발행 (커밋 후 리스너가 Kafka 전송)
        eventPublisher.publishEvent(new PaymentCompletedEvent(
                new PaymentEvent(
                        payment.getUserId(),
                        payment.getOrderId(),
                        payment.getDomain(),
                        payment.getTargetId(),
                        payment.getAmount())));
    }

    @Transactional
    public void markAsFailed(Long paymentId) {
        try {
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment != null) {
                payment.fail();
            }
        } catch (Exception e) {
            log.error("결제 실패 상태 업데이트 중 오류: paymentId={}", paymentId, e);
        }
    }

    @Transactional(readOnly = true)
    public List<Payment> findMyPayments(int userId) {
        log.info("결제 내역 조회: userId={}", userId);
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
