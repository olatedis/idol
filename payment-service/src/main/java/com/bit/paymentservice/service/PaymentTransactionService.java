package com.bit.paymentservice.service;

import com.bit.paymentservice.domain.dto.PaymentConfirmDto;
import com.bit.paymentservice.domain.dto.PaymentEvent;
import com.bit.paymentservice.domain.entity.Payment;
import com.bit.paymentservice.domain.enumtype.PaymentStatus;
import com.bit.paymentservice.domain.event.PaymentCompletedEvent;
import com.bit.paymentservice.infra.persistence.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

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
                        payment.getAmount(),
                        payment.deserializeReservationIds())));
    }

    @Transactional
    public void markAsFailed(Long paymentId) {
        try {
            paymentRepository.findById(paymentId).ifPresent(Payment::fail);
        } catch (Exception e) {
            log.error("결제 실패 상태 업데이트 중 오류: paymentId={}", paymentId, e);
        }
    }
}