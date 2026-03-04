package com.bit.reserveservice.api;

import com.bit.reserveservice.application.ReservationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // 예매 요청 (본인 인증 필수)
    @PostMapping
    public int reserve(
            @RequestHeader("X-User-Id") int userId, // Gateway에서 검증된 사용자 ID
            @RequestParam int concertId,
            @RequestParam int seatId,
            @RequestParam int price
    ) {
        return reservationService
                .reserve(userId, concertId, seatId, price);
    }

    // 예약 취소 (본인 취소)
    @DeleteMapping("/{reservationId}")
    public void cancelReservation(
            @RequestHeader("X-User-Id") int userId,
            @PathVariable int reservationId
    ) {
        reservationService.cancel(userId, reservationId);
    }
}
