package com.bit.reserveservice.api;

import com.bit.reserveservice.application.ReservationCommandService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationCommandService reservationCommandService;

    public ReservationController(ReservationCommandService reservationCommandService) {
        this.reservationCommandService = reservationCommandService;
    }

    // 예매 요청 (본인 인증 필수)
    @PostMapping
    public int reserve(
            @RequestHeader("X-User-Id") int userId, // Gateway에서 검증된 사용자 ID
            @RequestParam int concertId,
            @RequestParam int seatId,
            @RequestParam int price
    ) {
        return reservationCommandService
                .reserve(userId, concertId, seatId, price);
    }
}
