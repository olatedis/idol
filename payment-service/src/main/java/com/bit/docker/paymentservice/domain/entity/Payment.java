package com.bit.docker.paymentservice.domain.entity;

import com.bit.docker.paymentservice.domain.enumtype.PaymentDomain;
import com.bit.docker.paymentservice.domain.enumtype.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "targetId")
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PaymentDomain domain;

    private Long targetId;      // 결제 대상 ID(콘서트, 구독)

    private Long userId;

    private int amount;     // 최종 결제 금액

    private String paymentKey;   // PG가 내려준 결제 키
    private String orderId;        // 우리 시스템 주문 번호

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime createdAt;

    protected Payment() {
    }

    private Payment(Long targetId, Long userId, int amount, PaymentDomain domain) {
        this.targetId = targetId;
        this.userId = userId;
        this.amount = amount;
        this.domain = domain;
        this.status = PaymentStatus.READY;
        this.createdAt = LocalDateTime.now();
    }

    public static Payment create(Long reservationId, Long userId, int amount,  PaymentDomain domain) {
        return new Payment(reservationId, userId, amount , domain);
    }

    public void complete() {
        this.status = PaymentStatus.COMPLETED;
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
    }
}
