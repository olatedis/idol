package com.bit.paymentservice.domain.entity;

import com.bit.paymentservice.domain.enumtype.PaymentDomain;
import com.bit.paymentservice.domain.enumtype.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "payment", uniqueConstraints = {
        @UniqueConstraint(columnNames = "orderId")
}, indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_order_id", columnList = "order_id"),
        @Index(name = "idx_status", columnList = "status")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    private PaymentDomain domain;

    private int targetId; // 결제 대상 ID(콘서트, 구독)

    private int userId;

    private int amount; // 최종 결제 금액

    private int agencyId; // 소속사 아이디 (매출 집계용)

    private String paymentKey; // PG가 내려준 결제 키

    @Column(unique = true, nullable = false)
    private String orderId; // 시스템 주문 번호 (Idempotency key)

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(columnDefinition = "JSON")
    private String reservationIds; // JSON 배열 형식으로 예약 ID 저장

    private LocalDateTime createdAt;

    private LocalDateTime completedAt; // 결제 완료 시간

    protected Payment() {
    }

    private Payment(String orderId, int targetId, int userId, int amount, PaymentDomain domain,
            List<Integer> reservationIds, int agencyId) {
        this.orderId = orderId;
        this.targetId = targetId;
        this.userId = userId;
        this.amount = amount;
        this.domain = domain;
        this.agencyId = agencyId;
        this.status = PaymentStatus.READY;
        this.createdAt = LocalDateTime.now();
        this.reservationIds = serializeReservationIds(reservationIds);
    }

    public static Payment ready(
            String orderId,
            int amount,
            PaymentDomain domain,
            int targetId,
            int userId,
            List<Integer> reservationIds,
            int agencyId) {
        Payment payment = new Payment();
        payment.orderId = orderId;
        payment.amount = amount;
        payment.domain = domain;
        payment.targetId = targetId;
        payment.agencyId = agencyId;
        payment.status = PaymentStatus.READY;
        payment.userId = userId;
        payment.createdAt = LocalDateTime.now();
        payment.reservationIds = serializeReservationIds(reservationIds);
        return payment;
    }

    private static String serializeReservationIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty())
            return "[]";
        try {
            return new ObjectMapper().writeValueAsString(ids);
        } catch (Exception e) {
            return "[]";
        }
    }

    public List<Integer> deserializeReservationIds() {
        if (reservationIds == null || reservationIds.isEmpty() || "[]".equals(reservationIds)) {
            return List.of();
        }
        try {
            return new ObjectMapper().readValue(reservationIds,
                    new com.fasterxml.jackson.core.type.TypeReference<>() {
                    });
        } catch (Exception e) {
            return List.of();
        }
    }

    public void complete(String paymentKey, int amount) {
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELED;
    }
}
