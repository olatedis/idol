package com.bit.docker.reserveservice.application;

import com.bit.docker.reserveservice.domain.dto.PaymentEvent;
import com.bit.docker.reserveservice.domain.entity.Reservation;
import com.bit.docker.reserveservice.infra.kafka.ReservationEventProducer;
import com.bit.docker.reserveservice.infra.repository.ReservationRepository;
import com.bit.docker.reserveservice.infra.redis.SeatLockRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class ReservationCommandService {

    private final ReservationRepository reservationRepository;
    private final SeatLockRepository seatLockRepository;
    private final ReservationEventProducer eventProducer;


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
                    "RESERVATION",          // ✅ 결제완료 consumer의 비교값과 일치
                    reservation.getId(),    // ✅ targetId = reservationId로 추천
                    price
            );

            // ✅ 결제 요청만 보냄
            eventProducer.publishPaymentRequested(event);

            return reservation.getId();

        } catch (Exception e) {
            try {
                seatLockRepository.unlock(concertId, seatId, userId);
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
            throw e;
        }
    }

}
