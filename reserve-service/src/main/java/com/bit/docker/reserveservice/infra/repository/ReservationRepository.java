package com.bit.docker.reserveservice.infra.repository;

import com.bit.docker.reserveservice.domain.entity.Reservation;
import com.bit.docker.reserveservice.domain.enumtype.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    Optional<Reservation> findByUserIdAndSeatIdAndStatus(int userId, int seatId, ReservationStatus reservationStatus);
}
