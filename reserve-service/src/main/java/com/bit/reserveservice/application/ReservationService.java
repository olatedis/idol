package com.bit.reserveservice.application;

import com.bit.reserveservice.domain.entity.Reservation;
import com.bit.reserveservice.infra.redis.SeatLockRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ReservationService {

    private final SeatLockRepository seatLockRepository;
    private final ReservationHandler reservationHandler;
    private final ReservationCancellationHandler cancellationHandler;


    public int reserve(int userId, int concertId, int seatId, int price) {
        // 1. 락 획득
        boolean locked = seatLockRepository.lock(concertId, seatId, userId);
        if (!locked) {
            throw new IllegalStateException("이미 선점된 좌석입니다.");
        }

        // 2. 별도 클래스에서 트랜잭션 처리
        return reservationHandler.saveDbReserve(userId, concertId, seatId, price);
    }

    public void cancel(int userId, int reservationId) {
        cancellationHandler.cancelReservationByUser(userId, reservationId);
    }

    public List<Reservation> findByUser(int userId) {
       return reservationHandler.findByUser(userId);
    }
}
