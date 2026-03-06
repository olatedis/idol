package com.bit.reserveservice.infra.repository;

import com.bit.reserveservice.domain.entity.Reservation;
import com.bit.reserveservice.domain.enumtype.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    Optional<Reservation> findByUserIdAndSeatIdAndStatus(int userId, int seatId, ReservationStatus reservationStatus);

    List<Reservation> findByStatusAndCreatedAtBefore(ReservationStatus status, LocalDateTime cutoff);

    Optional<Reservation> findByConcertIdAndSeatId(int concertId, int seatId);
}
