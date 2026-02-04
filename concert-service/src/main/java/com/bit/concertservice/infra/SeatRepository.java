package com.bit.concertservice.infra;


import com.bit.concertservice.domain.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Integer> {

    List<Seat> findByConcertId(int concertId);
}
