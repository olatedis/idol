package com.bit.reserveservice.application;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ReservationExpirationController {

    private final ReservationExpirationScheduler scheduler;

    ReservationExpirationController(ReservationExpirationScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @GetMapping("/admin/expire-reservations")
    public String expireReservations() {
        scheduler.expirePendingReservationsManual();
        return "예약 만료 처리 실행됨";
    }
}
