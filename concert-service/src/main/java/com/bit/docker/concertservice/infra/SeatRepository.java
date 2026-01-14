package com.bit.docker.concertservice.infra;


import com.bit.docker.concertservice.domain.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByConcertId(Long concertId);
}
