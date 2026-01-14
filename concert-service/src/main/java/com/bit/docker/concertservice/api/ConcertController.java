package com.bit.docker.concertservice.api;

import com.bit.docker.concertservice.application.ConcertQueryService;
import com.bit.docker.concertservice.domain.entity.Concert;
import com.bit.docker.concertservice.domain.entity.Seat;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/concerts")
public class ConcertController {

    private final ConcertQueryService concertQueryService;

    public ConcertController(ConcertQueryService concertQueryService) {
        this.concertQueryService = concertQueryService;
    }

    @GetMapping
    public List<Concert> concerts() {
        return concertQueryService.getConcerts();
    }

    @GetMapping("/{concertId}")
    public Concert concert(@PathVariable Long concertId) {
        return concertQueryService.getConcert(concertId);
    }

    @GetMapping("/{concertId}/seats")
    public List<Seat> seats(@PathVariable Long concertId) {
        return concertQueryService.getSeats(concertId);
    }
}
