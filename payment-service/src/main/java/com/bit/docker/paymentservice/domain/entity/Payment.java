package com.bit.docker.paymentservice.domain.entity;

import com.bit.docker.paymentservice.domain.enumtype.PaymentDomain;
import com.bit.docker.paymentservice.domain.enumtype.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "targetId")
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    private PaymentDomain domain;

    private int targetId;      // 결제 대상 ID(콘서트, 구독)

    private int userId;

    private int amount;     // 최종 결제 금액

    private String paymentKey;   // PG가 내려준 결제 키
    private String orderId;        // 시스템 주문 번호

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime createdAt;

    protected Payment() {
    }

    private Payment(String orderId, int targetId, int userId, int amount, PaymentDomain domain) {
        this.orderId = orderId;
        this.targetId = targetId;
        this.userId = userId;
        this.amount = amount;
        this.domain = domain;
        this.status = PaymentStatus.READY;
        this.createdAt = LocalDateTime.now();
    }

    public static Payment ready(
            String orderId,
            int amount,
            PaymentDomain domain,
            int targetId,
            int userId
    ) {
        Payment payment = new Payment();
        payment.orderId = orderId;
        payment.amount = amount;
        payment.domain = domain;
        payment.targetId = targetId;
        payment.status = PaymentStatus.READY;
        payment.userId = userId;
        payment.createdAt = LocalDateTime.now();
        return payment;
    }


    public void complete(String paymentKey, int amount) {
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.status = PaymentStatus.COMPLETED;
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
    }
}
