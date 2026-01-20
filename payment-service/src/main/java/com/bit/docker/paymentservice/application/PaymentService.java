package com.bit.docker.paymentservice.application;

import com.bit.docker.paymentservice.domain.dto.TossConfirmRequest;
import com.bit.docker.paymentservice.domain.dto.TossConfirmResponse;
import com.bit.docker.paymentservice.domain.entity.Payment;
import com.bit.docker.paymentservice.infra.kafka.PaymentEventProducer;
import com.bit.docker.paymentservice.infra.persistence.PaymentRepository;
import com.bit.docker.paymentservice.infra.toss.TossPgClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer eventProducer;
    private final TossPgClient tossPgClient;

    @Transactional
    public void confirmPayment(
            String paymentKey,
            String orderId,
            int amount
    ) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("결제 없음"));

        TossConfirmResponse response = tossPgClient.confirm(
                new TossConfirmRequest(paymentKey, orderId, amount)
        );

        if (!"DONE".equals(response.getStatus())) {
            payment.fail();
            return;
        }

        payment.complete(response.getPaymentKey(),  response.getTotalAmount());

        // 결제 완료 이벤트 발행
        eventProducer.publishPaymentCompleted(payment);
    }


}
