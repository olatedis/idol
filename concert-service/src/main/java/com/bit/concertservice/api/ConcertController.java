package com.bit.concertservice.api;

import com.bit.concertservice.application.ConcertCommandService;
import com.bit.concertservice.application.ConcertQueryService;
import com.bit.concertservice.domain.dto.ConcertCreateRequest;
import com.bit.concertservice.domain.dto.ConcertUpdateRequest;
import com.bit.concertservice.domain.entity.Concert;
import com.bit.concertservice.domain.entity.Seat;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/concerts")
public class ConcertController {

    private final ConcertQueryService concertQueryService;
    private final ConcertCommandService concertCommandService;

    @PostMapping
    public ResponseEntity<Integer> createConcert(@RequestBody ConcertCreateRequest request) {
        int concertId = concertCommandService.createConcert(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(concertId);
    }
    @PutMapping("/{concertId}")
    public ResponseEntity<Void> updateConcert(@PathVariable int concertId, @RequestBody ConcertUpdateRequest request) {
        concertCommandService.updateConcert(concertId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{concertId}")
    public ResponseEntity<Void> deleteConcert(@PathVariable int concertId) {
        concertCommandService.deleteConcert(concertId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{concertId}/deactivate")
    public ResponseEntity<Void> deactivateConcert(@PathVariable int concertId) {
        concertCommandService.deactivateConcert(concertId);
        return ResponseEntity.ok().build();
    }
    @GetMapping
    public List<Concert> concerts(@RequestParam(name = "groupId", required = false) Integer groupId) {
        if (groupId == null) return concertQueryService.getConcerts();
        return concertQueryService.getConcertsByGroup(groupId);
    }

    @GetMapping("/{concertId}")
    public Concert concert(@PathVariable int concertId) {
        return concertQueryService.getConcert(concertId);
    }

    @GetMapping("/{concertId}/seats")
    public List<Seat> seats(@PathVariable int concertId) {
        return concertQueryService.getSeats(concertId);
    }
}
