package com.bit.concertservice.domain.entity;

import com.bit.concertservice.domain.enumtype.SeatGrade;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Getter
@Table(
        name = "seat"
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
    @JsonIgnore
    private Concert concert;

    private boolean locked = false;

    private Integer lockedBy;

    private LocalDateTime lockedAt;

    private Integer reservedBy;  // 결제 완료된 예약자

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

    public void reserve(int userId) {
        this.reservedBy = userId;
        this.unlock();  // 잠금 해제
    }

    public boolean isLocked() { return this.locked; }

}

