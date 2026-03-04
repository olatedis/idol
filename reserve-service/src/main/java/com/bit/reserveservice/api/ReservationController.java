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
    @GetMapping
    public int reserve(
            @RequestHeader("X-User-Id") int userId,
            @RequestParam int concertId,
            @RequestParam int seatId,
            @RequestParam int price
    ) {
        return reservationService
                .reserve(userId, concertId, seatId, price);
    }

    // 예약 취소 (본인 취소)
    @GetMapping("/{reservationId}")
    public void cancelReservation(
            @RequestHeader("X-User-Id") int userId,
            @PathVariable int reservationId
    ) {
        reservationService.cancel(userId, reservationId);
    }
}
