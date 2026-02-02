package com.bit.docker.concertservice.application;

import com.bit.docker.concertservice.domain.entity.Concert;
import com.bit.docker.concertservice.domain.entity.Seat;
import com.bit.docker.concertservice.infra.ConcertRepository;
import com.bit.docker.concertservice.infra.SeatRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConcertQueryService {

    private final ConcertRepository concertRepository;
    private final SeatRepository seatRepository;

    public ConcertQueryService(
            ConcertRepository concertRepository,
            SeatRepository seatRepository
    ) {
        this.concertRepository = concertRepository;
        this.seatRepository = seatRepository;
    }

    public List<Concert> getConcerts() {
        return concertRepository.findAll();
    }

    public List<Concert> getConcertsByAgency(int agencyId) {
        return concertRepository.findByAgencyIdAndActiveTrue(agencyId);
    }

    public Concert getConcert(int concertId) {
        return concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("콘서트가 존재하지 않습니다."));
    }

    public List<Seat> getSeats(int concertId) {
        return seatRepository.findByConcertId(concertId);
    }
}
