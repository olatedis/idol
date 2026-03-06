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
            
            // 1. 기존 예약 확인 (같은 좌석의 다른 사용자 예약 포함)
            var existingReservation = reservationRepository.findByConcertIdAndSeatId(concertId, seatId);
            
            if (existingReservation.isPresent()) {
                Reservation existing = existingReservation.get();
                
                // 기존 예약이 CANCELED 상태면 재활용
                if (existing.getStatus() == com.bit.reserveservice.domain.enumtype.ReservationStatus.CANCELED) {
                    log.info("취소된 예약 재활용: reservationId={}, concertId={}, seatId={}, userId={}", 
                            existing.getId(), concertId, seatId, userId);
                    existing.updateForReuse(price);
                    // userId 업데이트 필요 시 추가 (현재는 기존 userId 유지)
                    reservation = existing;
                } else {
                    // PENDING 또는 COMPLETED 상태면 예약 불가
                    throw new IllegalStateException("이미 예약된 좌석입니다. 다시 선택해주세요.");
                }
            } else {
                // 새로운 예약 생성
                reservation = Reservation.create(userId, concertId, seatId, price);
            }
            
            // 2. DB 저장
            reservationRepository.save(reservation);
            log.info("예약 저장 완료: reservationId={}, userId={}, concertId={}, seatId={}", 
                    reservation.getId(), userId, concertId, seatId);

            // 3. 락 유효성 재확인 (TTL 만료 방지)
            if (!seatLockRepository.verifyLock(concertId, seatId, userId)) {
                throw new IllegalStateException("예약 시간이 초과되었습니다. 다시 시도해주세요.");
            }

            PaymentEvent event = new PaymentEvent(
                    userId,
                    null,
                    "RESERVATION",
                    seatId,
                    price,
                    Collections.singletonList(reservation.getId()),
                    Collections.singletonList(seatId)
            );

            // 4. 이벤트 발행
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
