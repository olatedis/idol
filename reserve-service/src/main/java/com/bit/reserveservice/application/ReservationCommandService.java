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

@Service
@AllArgsConstructor
@Slf4j
public class ReservationCommandService {

    private final ReservationRepository reservationRepository;
    private final SeatLockRepository seatLockRepository;
    private final ApplicationEventPublisher eventPublisher; // 변경됨


    @Transactional
    public int reserve(int userId, int concertId, int seatId, int price) {

        boolean locked = seatLockRepository.lock(concertId, seatId, userId);
        if (!locked) {
            throw new IllegalStateException("이미 선점된 좌석입니다.");
        }

        Reservation reservation = null;
        try {
            reservation = Reservation.create(userId, concertId, seatId, price);
            reservationRepository.save(reservation);

            PaymentEvent event = new PaymentEvent(
                    userId,
                    null,
                    "RESERVATION",
                    seatId,
                    price
            );

            // ✅ 이벤트 발행 (커밋 후 실행됨)
            eventPublisher.publishEvent(new ReservationCreatedEvent(event));

            return reservation.getId();

        } catch (Exception e) {
            try {
                seatLockRepository.unlock(concertId, seatId);
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
            throw e;
        }
    }

}
