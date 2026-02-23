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

    // 콘서트 생성 (관리자/소속사 전용)
    @PostMapping
    public ResponseEntity<Integer> createConcert(
            @RequestHeader("X-Role") String role,
            @RequestBody ConcertCreateRequest request
    ) {
        if (!"ADMIN".equals(role) && !"AGENCY".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        int concertId = concertCommandService.createConcert(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(concertId);
    }

    // 콘서트 수정 (관리자/소속사 전용)
    @PutMapping("/{concertId}")
    public ResponseEntity<Void> updateConcert(
            @RequestHeader("X-Role") String role,
            @PathVariable int concertId, 
            @RequestBody ConcertUpdateRequest request
    ) {
        if (!"ADMIN".equals(role) && !"AGENCY".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        concertCommandService.updateConcert(concertId, request);
        return ResponseEntity.ok().build();
    }

    // 콘서트 삭제 (관리자/소속사 전용)
    @DeleteMapping("/{concertId}")
    public ResponseEntity<Void> deleteConcert(
            @RequestHeader("X-Role") String role,
            @PathVariable int concertId
    ) {
        if (!"ADMIN".equals(role) && !"AGENCY".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        concertCommandService.deleteConcert(concertId);
        return ResponseEntity.noContent().build();
    }

    // 콘서트 비활성화 (관리자/소속사 전용)
    @PostMapping("/{concertId}/deactivate")
    public ResponseEntity<Void> deactivateConcert(
            @RequestHeader("X-Role") String role,
            @PathVariable int concertId
    ) {
        if (!"ADMIN".equals(role) && !"AGENCY".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        concertCommandService.deactivateConcert(concertId);
        return ResponseEntity.ok().build();
    }

    // 콘서트 목록 조회 (누구나 가능)
    @GetMapping
    public List<Concert> concerts(@RequestParam(name = "groupId", required = false) Integer groupId) {
        if (groupId == null) return concertQueryService.getConcerts();
        return concertQueryService.getConcertsByGroup(groupId);
    }

    // 콘서트 상세 조회 (누구나 가능)
    @GetMapping("/{concertId}")
    public Concert concert(@PathVariable int concertId) {
        return concertQueryService.getConcert(concertId);
    }

    // 좌석 조회 (누구나 가능)
    @GetMapping("/{concertId}/seats")
    public List<Seat> seats(@PathVariable int concertId) {
        return concertQueryService.getSeats(concertId);
    }
}
