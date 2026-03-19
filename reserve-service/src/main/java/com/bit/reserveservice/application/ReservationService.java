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

    public java.util.List<Integer> reserveMany(int userId, int concertId, java.util.List<Integer> seatIds, int price) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("좌석 목록이 비어 있습니다.");
        }

        // 모든 좌석에 대해 락 확보
        for (int seatId : seatIds) {
            boolean locked = seatLockRepository.lock(concertId, seatId, userId);
            if (!locked) {
                throw new IllegalStateException("이미 선점된 좌석이 포함되어 있습니다. seatId=" + seatId);
            }
        }

        // 트랜잭션으로 저장 + 한 번에 결제 요청 이벤트 발행
        return reservationHandler.saveDbReserveBulk(userId, concertId, seatIds, price);
    }

    public void cancel(int userId, int reservationId) {
        cancellationHandler.cancelReservationByUser(userId, reservationId);
    }

    public List<Reservation> findByUser(int userId) {
       return reservationHandler.findByUser(userId);
    }
}
