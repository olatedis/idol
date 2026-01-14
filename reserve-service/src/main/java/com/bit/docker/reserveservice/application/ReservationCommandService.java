package com.bit.docker.reserveservice.application;

import com.bit.docker.reserveservice.domain.entity.Reservation;
import com.bit.docker.reserveservice.infra.kafka.ReservationEventProducer;
import com.bit.docker.reserveservice.infra.persistence.ReservationRepository;
import com.bit.docker.reserveservice.infra.redis.SeatLockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationCommandService {

    private final ReservationRepository reservationRepository;
    private final SeatLockRepository seatLockRepository;
    private final ReservationEventProducer eventProducer;

    public ReservationCommandService(
            ReservationRepository reservationRepository,
            SeatLockRepository seatLockRepository,
            ReservationEventProducer eventProducer
    ) {
        this.reservationRepository = reservationRepository;
        this.seatLockRepository = seatLockRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public Long reserve(Long userId, Long concertId, Long seatId) {

        boolean locked = seatLockRepository.lock(concertId, seatId, userId);
        if (!locked) {
            throw new IllegalStateException("이미 선점된 좌석입니다.");
        }

        Reservation reservation =
                Reservation.create(userId, concertId, seatId);

        reservationRepository.save(reservation);

        eventProducer.publishReservationCreated(reservation.getId());

        return reservation.getId();
    }
}
