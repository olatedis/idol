package com.bit.docker.reserveservice.domain.entity;

import com.bit.docker.reserveservice.domain.enumtype.ReservationStatus;
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
    private Long id;

    private Long userId;

    private Long concertId;

    private Long seatId;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private LocalDateTime createdAt;

    protected Reservation() {
    }

    private Reservation(Long userId, Long concertId, Long seatId) {
        this.userId = userId;
        this.concertId = concertId;
        this.seatId = seatId;
        this.status = ReservationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public static Reservation create(Long userId, Long concertId, Long seatId) {
        return new Reservation(userId, concertId, seatId);
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELED;
    }
}

