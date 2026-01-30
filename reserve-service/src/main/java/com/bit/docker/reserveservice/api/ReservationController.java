package com.bit.docker.reserveservice.api;

import com.bit.docker.reserveservice.application.ReservationCommandService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationCommandService reservationCommandService;

    public ReservationController(ReservationCommandService reservationCommandService) {
        this.reservationCommandService = reservationCommandService;
    }

    @PostMapping
    public int reserve(
            @RequestParam int userId,
            @RequestParam int concertId,
            @RequestParam int seatId,
            @RequestParam int price
    ) {
        return reservationCommandService
                .reserve(userId, concertId, seatId, price);
    }
}