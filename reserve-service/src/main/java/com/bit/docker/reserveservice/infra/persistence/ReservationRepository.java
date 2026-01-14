package com.bit.docker.reserveservice.infra.persistence;

import com.bit.docker.reserveservice.domain.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
