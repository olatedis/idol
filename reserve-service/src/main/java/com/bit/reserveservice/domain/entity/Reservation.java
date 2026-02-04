package com.bit.reserveservice.domain.entity;

import com.bit.reserveservice.domain.enumtype.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "reservation",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"concertId", "seatId"})
        }
)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int userId;

    private int concertId;

    private int seatId;

    private int price;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private LocalDateTime createdAt;

    protected Reservation() {
    }

    private Reservation(int userId, int concertId, int seatId, int price) {
        this.userId = userId;
        this.concertId = concertId;
        this.seatId = seatId;
        this.price = price;
        this.status = ReservationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public static Reservation create(int userId, int concertId, int seatId, int price) {
        return new Reservation(userId, concertId, seatId, price);
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELED;
    }
}

