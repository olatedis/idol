package com.bit.reserveservice.application;

import com.bit.reserveservice.domain.entity.Reservation;
import com.bit.reserveservice.domain.enumtype.ReservationStatus;
import com.bit.reserveservice.infra.kafka.ReservationEventProducer;
import com.bit.reserveservice.infra.redis.SeatLockRepository;
import com.bit.reserveservice.infra.repository.ReservationRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class ReservationExpirationScheduler {

    private final ReservationRepository reservationRepository;
    private final SeatLockRepository seatLockRepository;
    private final ReservationEventProducer eventProducer;

    @Value("${reservation.lock-expire-minutes}")
    private int expireMinutes;

    @Scheduled(fixedDelayString = "${reservation.expire-check-ms:60000}")
    @Transactional
    public void expirePendingReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(expireMinutes);
        List<Reservation> expired = reservationRepository.findByStatusAndCreatedAtBefore(ReservationStatus.PENDING, cutoff);
        if (expired.isEmpty()) {
            return;
        }

        for (Reservation r : expired) {
            try {
                r.cancel();
                reservationRepository.save(r);

                try {
                    seatLockRepository.unlock(r.getConcertId(), r.getSeatId());
                } catch (Exception e) {
                    log.warn("좌석 잠금 해제 실패(만료): concert={}, seat={}, error={}", r.getConcertId(), r.getSeatId(), e.getMessage());
                }

                log.info("만료로 예약 취소 처리: reservationId={}, userId={}, concert={}, seat={}", r.getId(), r.getUserId(), r.getConcertId(), r.getSeatId());
            } catch (Exception e) {
                log.error("예약 만료 처리 중 오류: reservationId={}, error={}", r.getId(), e.getMessage(), e);
            }
        }
    }
}
