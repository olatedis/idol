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
        try {
            return reservationService
                    .reserve(userId, requestReservation.getConcertId(), requestReservation.getSeatId(), requestReservation.getPrice());
        } catch (IllegalStateException | IllegalArgumentException e) {
            // 클라이언트 오류로 처리
            throw new ReservationException(e.getMessage());
        }
    }

    // 예약 취소 (본인 취소)
    @GetMapping("/{reservationId}")
    public void cancelReservation(
            @RequestHeader("X-User-Id") int userId,
            @PathVariable int reservationId
    ) {
        reservationService.cancel(userId, reservationId);
    }

    // 에러 응답 변환
    @ExceptionHandler(ReservationException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public String handleReservationException(ReservationException ex) {
        return ex.getMessage();
    }

    // 전용 예외
    public static class ReservationException extends RuntimeException {
        public ReservationException(String msg) { super(msg); }
    }
}
