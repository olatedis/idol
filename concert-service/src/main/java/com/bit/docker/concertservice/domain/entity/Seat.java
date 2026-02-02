package com.bit.docker.concertservice.domain.entity;

import com.bit.docker.concertservice.domain.enumtype.SeatGrade;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Getter
@Table(
        name = "seat",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"concert_id", "seat_number"})
        }
)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String seatNumber;

    @Enumerated(EnumType.STRING)
    private SeatGrade grade;

    private int price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id")
    private Concert concert;

    private boolean locked = false;

    private Integer lockedBy;

    private LocalDateTime lockedAt;

    protected Seat() {}

    public Seat(String seatNumber, SeatGrade grade, int price, Concert concert) {
        this.seatNumber = seatNumber;
        this.grade = grade;
        this.price = price;
        this.concert = concert;
    }

    public void lock(int userId) {
        this.locked = true;
        this.lockedBy = userId;
        this.lockedAt = LocalDateTime.now();
    }

    public void unlock() {
        this.locked = false;
        this.lockedBy = null;
        this.lockedAt = null;
    }

    public boolean isLocked() { return this.locked; }

}

