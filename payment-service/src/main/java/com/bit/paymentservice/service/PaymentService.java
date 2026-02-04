package com.bit.paymentservice.service;

import com.bit.paymentservice.domain.dto.*;
import com.bit.paymentservice.domain.entity.Payment;
import com.bit.paymentservice.domain.enumtype.PaymentStatus;
import com.bit.paymentservice.infra.persistence.PaymentRepository;
import com.bit.paymentservice.infra.toss.TossPgClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducerService  paymentEventProducerService;
    private final TossPgClient tossPgClient;


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
                request.getUserId()
        );

        paymentRepository.save(payment);
        log.info("결제 준비 완료: orderId={}, userId={}, amount={}, domain={}", 
                orderId, request.getUserId(), request.getAmount(), request.getDomain());

        return new PaymentCreateResponse(orderId, payment.getAmount());
    }

    @Transactional
    public void confirm(PaymentConfirmDto dto, int requestUserId) {
        log.info("결제 승인 요청: orderId={}, requestUserId={}", dto.getOrderId(), requestUserId);

        Payment payment = paymentRepository.findByOrderId(dto.getOrderId())
                .orElseThrow(() -> {
                    log.error("주문 없음: orderId={}", dto.getOrderId());
                    return new IllegalArgumentException("주문 없음");
                });

        // 사용자 검증 - 자신의 결제만 승인 가능
        if (payment.getUserId() != requestUserId) {
            log.warn("권한 없는 결제 승인 시도: orderId={}, paymentUserId={}, requestUserId={}", 
                    dto.getOrderId(), payment.getUserId(), requestUserId);
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        // 상태가 READY인지 검증 (Idempotency)
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

        try {
            // 토스페이먼츠 승인 API 호출
            log.info("토스페이먼츠 승인 API 호출: orderId={}, amount={}", dto.getOrderId(), dto.getAmount());
            TossConfirmResponse response = tossPgClient.confirm(new TossConfirmRequest(
                    dto.getPaymentKey(),
                    dto.getOrderId(),
                    dto.getAmount()
            ));

            // 토스 측 상태가 DONE 이면 승인 성공
            if ("DONE".equals(response.getStatus())) {
                payment.complete(response.getPaymentKey(), payment.getAmount());
                log.info("결제 승인 완료: orderId={}, paymentKey={}", dto.getOrderId(), response.getPaymentKey());

                // 승인 성공 Kafka 이벤트 발행
                try {
                    paymentEventProducerService.publish(
                            new PaymentEvent(
                                    payment.getUserId(),
                                    payment.getOrderId(),
                                    payment.getDomain(),
                                    payment.getTargetId(),
                                    payment.getAmount()
                            )
                    );
                    log.info("결제 이벤트 발행 완료: orderId={}, userId={}", dto.getOrderId(), payment.getUserId());
                } catch (Exception e) {
                    log.error("카프카 이벤트 발행 실패: orderId={}, error={}", dto.getOrderId(), e.getMessage());
                    // 이벤트 발행 실패해도 결제는 완료로 표시 (나중에 재시도 가능)
                }
            } else {
                payment.fail();
                log.warn("토스페이먼츠 결제 거절: orderId={}, tossStatus={}", dto.getOrderId(), response.getStatus());
                throw new IllegalStateException("결제가 거절되었습니다.");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            payment.fail();
            log.error("토스페이먼츠 API 호출 실패: orderId={}, error={}", dto.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("결제 승인 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}
