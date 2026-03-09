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
public class ReservationExpirationHandler {

    private final ReservationRepository reservationRepository;
    private final SeatLockRepository seatLockRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireReservation(int reservationId) {
        log.info("예약 만료 처리 시작: reservationId={}", reservationId);
        
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다."));

        log.info("예약 상태 확인: reservationId={}, status={}, createdAt={}", 
                r.getId(), r.getStatus(), r.getCreatedAt());

        if (r.getStatus() != ReservationStatus.PENDING) {
            log.info("이미 처리된 예약: reservationId={}, status={}", r.getId(), r.getStatus());
            return; // 이미 처리됨
        }

        r.cancel();
        reservationRepository.save(r);
        log.info("예약 상태 CANCELED로 변경 완료: reservationId={}", r.getId());

        // Redis 락 해제 (트랜잭션과 무관하게 실행)
        try {
            log.info("Redis 락 해제 시도: concertId={}, seatId={}", r.getConcertId(), r.getSeatId());
            seatLockRepository.unlock(r.getConcertId(), r.getSeatId());
            log.info("Redis 락 해제 완료: concertId={}, seatId={}", r.getConcertId(), r.getSeatId());
        } catch (Exception e) {
            log.warn("좌석 잠금 해제 실패(만료): concert={}, seat={}, error={}", r.getConcertId(), r.getSeatId(), e.getMessage());
        }

        log.info("만료로 예약 취소 처리 완료: reservationId={}, userId={}", r.getId(), r.getUserId());
    }
}
