package com.bit.concertservice.application;

import com.bit.concertservice.domain.entity.Concert;
import com.bit.concertservice.domain.entity.Seat;
import com.bit.concertservice.infra.ConcertRepository;
import com.bit.concertservice.infra.SeatRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ConcertQueryService {

    private final ConcertRepository concertRepository;
    private final SeatRepository seatRepository;

    public List<Concert> getConcerts() {
        return concertRepository.findAll();
    }

    public List<Concert> getConcertsByGroup(int groupId) {
        return concertRepository.findByGroupIdAndActiveTrue(groupId);
    }

    public Concert getConcert(int concertId) {
        return concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("콘서트가 존재하지 않습니다."));
    }

    public List<Seat> getSeats(int concertId) {
        return seatRepository.findByConcertId(concertId);
    }
}
