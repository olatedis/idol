package com.bit.reserveservice.api;

import com.bit.reserveservice.application.ReservationService;
import com.bit.reserveservice.domain.dto.RequestReservation;
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
            @RequestHeader("X-User-Id") int userId,
            @RequestBody RequestReservation requestReservation
    ) {
        return reservationService
                .reserve(userId, requestReservation.getConcertId(), requestReservation.getSeatId(), requestReservation.getPrice());
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
