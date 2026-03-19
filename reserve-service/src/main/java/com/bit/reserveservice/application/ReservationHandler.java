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

    private Reservation persistReservation(int userId, int concertId, int seatId, int price) {
        Reservation reservation;

        // 1. 기존 예약 확인 (같은 좌석의 다른 사용자 예약 포함)
        var existingReservation = reservationRepository.findByConcertIdAndSeatId(concertId, seatId);

        if (existingReservation.isPresent()) {
            Reservation existing = existingReservation.get();

            // 기존 예약이 CANCELED 상태면 재활용
            if (existing.getStatus() == com.bit.reserveservice.domain.enumtype.ReservationStatus.CANCELED) {
                log.info("취소된 예약 재활용: reservationId={}, concertId={}, seatId={}, userId= {}",
                        existing.getId(), concertId, seatId, userId);
                existing.updateForReuse(price);
                reservation = existing;
            } else {
                // PENDING 또는 COMPLETED 상태면 예약 불가
                throw new IllegalStateException("이미 예약된 좌석입니다. 다시 선택해주세요.");
            }
        } else {
            // 새로운 예약 생성
            reservation = Reservation.create(userId, concertId, seatId, price);
        }

        reservationRepository.save(reservation);
        return reservation;
    }

    @Transactional
    public int saveDbReserve(int userId, int concertId, int seatId, int price) {
        Reservation reservation = persistReservation(userId, concertId, seatId, price);
        log.info("예약 저장 완료: reservationId={}, userId={}, concertId={}, seatId={}",
                reservation.getId(), userId, concertId, seatId);

        // 3. 락 유효성 재확인 (TTL 만료 방지)
        if (!seatLockRepository.verifyLock(concertId, seatId, userId)) {
            throw new IllegalStateException("예약 시간이 초과되었습니다. 다시 시도해주세요.");
        }

        PaymentEvent event = new PaymentEvent(
                userId,
                null,
                "CONCERT",
                seatId,
                price,
                0, // agencyId
                java.util.Collections.singletonList(reservation.getId()),
                java.util.Collections.singletonList(seatId));

        // 4. 이벤트 발행
        eventPublisher.publishEvent(new ReservationCreatedEvent(event));

        return reservation.getId();
    }

    @Transactional
    public java.util.List<Integer> saveDbReserveBulk(int userId, int concertId, java.util.List<Integer> seatIds, int price) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("좌석이 선택되지 않았습니다.");
        }

        java.util.List<Integer> reservationIds = new java.util.ArrayList<>();
        java.util.List<Integer> savedSeatIds = new java.util.ArrayList<>();

        for (int seatId : seatIds) {
            Reservation reservation = persistReservation(userId, concertId, seatId, price);
            log.info("예약 저장 완료 (bulk): reservationId={}, userId={}, concertId={}, seatId={}",
                    reservation.getId(), userId, concertId, seatId);

            // 락 유효성 재확인
            if (!seatLockRepository.verifyLock(concertId, seatId, userId)) {
                throw new IllegalStateException("예약 시간이 초과되었습니다. 다시 시도해주세요.");
            }

            reservationIds.add(reservation.getId());
            savedSeatIds.add(seatId);
        }

        PaymentEvent event = new PaymentEvent(
                userId,
                null,
                "CONCERT",
                concertId,
                price * seatIds.size(),
                0,
                reservationIds,
                savedSeatIds);

        eventPublisher.publishEvent(new ReservationCreatedEvent(event));
        return reservationIds;
    }

    // 조회용 헬퍼
    public java.util.List<com.bit.reserveservice.domain.entity.Reservation> findByUser(int userId) {
        return reservationRepository.findAllByUserId(userId);
    }
}
