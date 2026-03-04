package com.bit.reserveservice.application;

import com.bit.reserveservice.domain.entity.Reservation;
import com.bit.reserveservice.domain.enumtype.ReservationStatus;
import com.bit.reserveservice.infra.redis.SeatLockRepository;
import com.bit.reserveservice.infra.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCancellationHandler {

    private final ReservationRepository reservationRepository;
    private final SeatLockRepository seatLockRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelReservationByUser(int userId, int reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다."));

        if (r.getUserId() != userId) {
            throw new SecurityException("자신의 예약만 취소할 수 있습니다.");
        }

        if (r.getStatus() != ReservationStatus.PENDING) {
            return; // 이미 처리됨
        }

        r.cancel();
        reservationRepository.save(r);

        try {
            seatLockRepository.unlock(r.getConcertId(), r.getSeatId());
        } catch (Exception e) {
            log.warn("좌석 잠금 해제 실패(사용자 취소): concert={}, seat={}, error={}", r.getConcertId(), r.getSeatId(), e.getMessage());
        }

        log.info("사용자 요청으로 예약 취소 완료: reservationId={}, userId={}", r.getId(), r.getUserId());
    }
}
