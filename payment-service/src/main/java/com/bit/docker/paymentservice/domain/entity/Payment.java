package com.bit.docker.paymentservice.domain.entity;

import com.bit.docker.paymentservice.domain.enumtype.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "reservationId")
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reservationId;

    private Long userId;

    private int amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime createdAt;

    protected Payment() {
    }

    private Payment(Long reservationId, Long userId, int amount) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.amount = amount;
        this.status = PaymentStatus.REQUESTED;
        this.createdAt = LocalDateTime.now();
    }

    public static Payment create(Long reservationId, Long userId, int amount) {
        return new Payment(reservationId, userId, amount);
    }

    public void complete() {
        this.status = PaymentStatus.COMPLETED;
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
    }
}
