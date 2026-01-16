package com.bit.docker.paymentservice.application;

import com.bit.docker.paymentservice.domain.dto.TossConfirmRequest;
import com.bit.docker.paymentservice.domain.dto.TossConfirmResponse;
import com.bit.docker.paymentservice.domain.entity.Payment;
import com.bit.docker.paymentservice.domain.enumtype.PaymentDomain;
import com.bit.docker.paymentservice.domain.enumtype.PaymentStatus;
import com.bit.docker.paymentservice.domain.policy.PaymentPolicy;
import com.bit.docker.paymentservice.infra.kafka.PaymentEventProducer;
import com.bit.docker.paymentservice.infra.persistence.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCommandService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer eventProducer;

    public PaymentCommandService(
            PaymentRepository paymentRepository,
            PaymentEventProducer eventProducer
    ) {
        this.paymentRepository = paymentRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public void createPayment(Long reservationId) {

        paymentRepository.findByReservationId(reservationId)
                .ifPresent(payment -> {
                    throw new IllegalStateException("이미 결제가 존재합니다.");
                });

        // 실제로는 reservation 정보 조회 or 금액 계산 필요
        Payment payment = Payment.create(reservationId, 1L, 10000, PaymentDomain.CONCERT);
        paymentRepository.save(payment);

        processPayment(payment);
    }

    private void processPayment(Payment payment) {

        boolean success = PaymentPolicy.validatePayable(payment);

        if (success) {
            payment.complete();
            eventProducer.publishPaymentCompleted(payment.getTargetId());
        } else {
            payment.fail();
            eventProducer.publishPaymentFailed(payment.getTargetId());
        }
    }

    @Transactional
    public void confirmPayment(
            String paymentKey,
            String orderId,
            Long amount
    ) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("결제 없음"));

        TossConfirmResponse response = tossPgClient.confirm(
                new TossConfirmRequest(paymentKey, orderId, amount)
        );

        if (!"DONE".equals(response.getStatus())) {
            payment.setStatus(PaymentStatus.FAILED);
            return;
        }

        payment.setPaymentKey(paymentKey);
        payment.setAmount(response.getTotalAmount());
        payment.setStatus(PaymentStatus.COMPLETED);

        // 결제 완료 이벤트 발행
        paymentEventProducer.publishCompleted(payment);
    }

}
