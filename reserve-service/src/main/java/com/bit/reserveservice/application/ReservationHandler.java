package com.bit.reserveservice.application;

import com.bit.reserveservice.domain.dto.PaymentEvent;
import com.bit.reserveservice.domain.entity.Reservation;
import com.bit.reserveservice.domain.event.ReservationCreatedEvent;
import com.bit.reserveservice.infra.repository.ReservationRepository;
import com.bit.reserveservice.infra.redis.SeatLockRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@AllArgsConstructor
@Slf4j
public class ReservationHandler {

    private final ReservationRepository reservationRepository;
    private final SeatLockRepository seatLockRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public int saveDbReserve(int userId, int concertId, int seatId, int price) {
        try {
            Reservation reservation;
            // 1. DB 저장
            reservation = Reservation.create(userId, concertId, seatId, price);
            reservationRepository.save(reservation);

            // 2. 락 유효성 재확인 (TTL 만료 방지)
            if (!seatLockRepository.verifyLock(concertId, seatId, userId)) {
                throw new IllegalStateException("예약 시간이 초과되었습니다. 다시 시도해주세요.");
            }

            PaymentEvent event = new PaymentEvent(
                    userId,
                    null,
                    "RESERVATION",
                    seatId,
                    price,
                    Collections.singletonList(reservation.getId())
            );

            // 3. 이벤트 발행
            eventPublisher.publishEvent(new ReservationCreatedEvent(event));

            return reservation.getId();

        } catch (Exception e) {
            // 실패 시 락 해제 (내 락일 때만 해제하는 게 좋지만, verifyLock 실패 시엔 이미 내 락이 아님)
            try {
                if (seatLockRepository.verifyLock(concertId, seatId, userId)) {
                    seatLockRepository.unlock(concertId, seatId);
                }
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
            throw e;
        }
    }
}
