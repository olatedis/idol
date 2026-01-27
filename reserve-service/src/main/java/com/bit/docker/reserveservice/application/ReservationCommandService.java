package com.bit.docker.reserveservice.application;

import com.bit.docker.reserveservice.domain.dto.PaymentEvent;
import com.bit.docker.reserveservice.domain.entity.Reservation;
import com.bit.docker.reserveservice.infra.kafka.ReservationEventProducer;
import com.bit.docker.reserveservice.infra.repository.ReservationRepository;
import com.bit.docker.reserveservice.infra.redis.SeatLockRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ReservationCommandService {

    private final ReservationRepository reservationRepository;
    private final SeatLockRepository seatLockRepository;
    private final ReservationEventProducer eventProducer;

    @Value("${concert.reservation.amount}")
    private int amount;

    @Transactional
    public int reserve(int userId, int concertId, int seatId) {

        boolean locked = seatLockRepository.lock(concertId, seatId, userId);
        if (!locked) {
            throw new IllegalStateException("이미 선점된 좌석입니다.");
        }

        Reservation reservation =
                Reservation.create(userId, concertId, seatId);

        reservationRepository.save(reservation);

        PaymentEvent event = new PaymentEvent(
                userId,
                null,
                "Reservation-service",
                seatId,
                amount
        );
        eventProducer.publishReservationCreated(event);

        return reservation.getId();
    }
}
