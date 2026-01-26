package com.bit.docker.paymentservice.service;

import com.bit.docker.paymentservice.domain.dto.*;
import com.bit.docker.paymentservice.domain.entity.Payment;
import com.bit.docker.paymentservice.domain.enumtype.PaymentStatus;
import com.bit.docker.paymentservice.infra.kafka.PaymentEventProducer;
import com.bit.docker.paymentservice.infra.persistence.PaymentRepository;
import com.bit.docker.paymentservice.infra.toss.TossPgClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentEventProducerService  paymentEventProducerService;
    private final TossPgClient tossPgClient;


    @Transactional
    public PaymentCreateResponse createPayment(PaymentCreateRequest request, int userId) {

        String orderId = UUID.randomUUID().toString();

        Payment payment = Payment.ready(
                orderId,
                request.getAmount(),
                request.getDomain(),
                request.getTargetId(),
                userId
        );

        paymentRepository.save(payment);

        return new PaymentCreateResponse(orderId, payment.getAmount());
    }

    @Transactional
    public void confirm(PaymentConfirmDto dto) {

        Payment payment = paymentRepository.findByOrderId(dto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("주문 없음"));

        // 상태가 READY인지 검증
        if (payment.getStatus() != PaymentStatus.READY) {
            throw new IllegalStateException("이미 처리된 주문");
        }

        // 금액 검증
        if (payment.getAmount() != (dto.getAmount())) {
            throw new IllegalArgumentException("금액 불일치");
        }

        // 토스페이먼츠 승인 API 호출
        TossConfirmResponse response =
                tossPgClient.confirm(new TossConfirmRequest(
                        dto.getPaymentKey(),
                        dto.getOrderId(),
                        dto.getAmount()
                ));

        // 토스 측 상태가 DONE 이면 승인 성공
        if ("DONE".equals(response.getStatus())) {
            payment.complete(response.getPaymentKey(), payment.getAmount());


            System.out.println("payment: "+payment);

//            try{
//                // 승인 성공 Kafka 이벤트 발행
//                paymentEventProducerService.publish(
//                        "payment.completed",
//                        new PaymentEvent(
//                                payment.getUserId(),
//                                payment.getOrderId(),
//                                payment.getDomain(),
//                                payment.getTargetId(),
//                                payment.getAmount()
//                        )
//                );
//
//            }catch (Exception e){
//                System.out.println("카프카 이벤트 발행 실패"+ e);
//            }
        } else {
            payment.fail();
        }
    }

}
