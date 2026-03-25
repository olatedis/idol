package com.bit.reserveservice.api;

import com.bit.reserveservice.application.ReservationService;
import com.bit.reserveservice.domain.dto.RequestReservation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
        // 로그를 찍어 호출 여부 확인
        log.info("예약 API 호출: userId={}, concertId={}, seatId={}, price={}",
                userId,
                requestReservation.getConcertId(),
                requestReservation.getSeatId(),
                requestReservation.getPrice());
        try {
            int savedId = reservationService
                    .reserve(userId, requestReservation.getConcertId(), requestReservation.getSeatId(), requestReservation.getPrice());
            log.info("예약 서비스 반환 id={}", savedId);
            return savedId;
        } catch (IllegalStateException | IllegalArgumentException e) {
            // 클라이언트 오류로 처리
            throw new ReservationException(e.getMessage());
        }
    }

    @PostMapping("/bulk")
    public java.util.List<Integer> reserveBulk(
            @RequestHeader("X-User-Id") int userId,
            @RequestBody RequestReservation requestReservation
    ) {
        if (requestReservation.getSeatIds() == null || requestReservation.getSeatIds().isEmpty()) {
            throw new ReservationException("seatIds가 비어 있습니다.");
        }

        log.info("예약 bulk API 호출: userId={}, concertId={}, seatIds={}, price={}",
                userId,
                requestReservation.getConcertId(),
                requestReservation.getSeatIds(),
                requestReservation.getPrice());

        try {
            java.util.List<Integer> ids = reservationService.reserveMany(
                    userId,
                    requestReservation.getConcertId(),
                    requestReservation.getSeatIds(),
                    requestReservation.getSeatPrices());
            log.info("예약 서비스 bulk 반환 ids={}", ids);
            return ids;
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new ReservationException(e.getMessage());
        }
    }

    // 예약 취소 (본인 취소)
    @DeleteMapping("/{reservationId}")
    public void cancelReservation(
            @RequestHeader("X-User-Id") int userId,
            @PathVariable int reservationId
    ) {
        reservationService.cancel(userId, reservationId);
    }

    // **디버그용**: 현재 사용자의 모든 예약 목록을 반환
    // 네트워크 탭으로 호출하면 실제 DB에 어떤 레코드가 있는지 확인할 수 있습니다.
    @GetMapping("/me")
    public java.util.List<com.bit.reserveservice.domain.entity.Reservation> myReservations(
            @RequestHeader("X-User-Id") int userId
    ) {
        log.info("예약 조회 API 호출 - userId={}", userId);
        return reservationService.findByUser(userId);
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
