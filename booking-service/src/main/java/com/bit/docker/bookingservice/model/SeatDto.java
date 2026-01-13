package com.bit.docker.bookingservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"concert_id", "seat_number"}
        )
)
public class SeatDto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String seatNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    private ConcertDto concert;
}

