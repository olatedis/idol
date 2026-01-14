package com.bit.docker.concertservice.domain.entity;

import com.bit.docker.concertservice.domain.enumtype.SeatGrade;
import jakarta.persistence.*;
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
    private Long id;

    private String seatNumber;

    @Enumerated(EnumType.STRING)
    private SeatGrade grade;

    private int price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id")
    private Concert concert;

    protected Seat() {}

    public Seat(String seatNumber, SeatGrade grade, int price, Concert concert) {
        this.seatNumber = seatNumber;
        this.grade = grade;
        this.price = price;
        this.concert = concert;
    }

}

