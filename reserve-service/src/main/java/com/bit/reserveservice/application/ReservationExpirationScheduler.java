package com.bit.reserveservice.application;

import com.bit.reserveservice.domain.entity.Reservation;
import com.bit.reserveservice.domain.enumtype.ReservationStatus;
import com.bit.reserveservice.infra.redis.SeatLockRepository;
import com.bit.reserveservice.infra.repository.ReservationRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationExpirationScheduler {

    private final ReservationRepository reservationRepository;
    private final SeatLockRepository seatLockRepository;
    // private final ReservationEventProducer eventProducer; // 사용 안 함

    @Value("${reservation.lock-expire-minutes:10}") // 기본값 10분 추가
    private int expireMinutes;

    @Scheduled(fixedDelayString = "${reservation.expire-check-ms:60000}")
    @SchedulerLock(name = "expirePendingReservations", lockAtLeastFor = "PT30S", lockAtMostFor = "PT50S")
    public void expirePendingReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(expireMinutes);
        
        // 트랜잭션 없이 조회 (OSIV 껐으므로 안전)
        List<Reservation> expired = reservationRepository.findByStatusAndCreatedAtBefore(ReservationStatus.PENDING, cutoff);
        
        if (expired.isEmpty()) {
            return;
        }

        log.info("만료된 예약 {}건 발견. 취소 처리 시작...", expired.size());

        for (Reservation r : expired) {
            try {
                // 개별 트랜잭션으로 처리
                expireReservation(r.getId());
            } catch (Exception e) {
                log.error("예약 만료 처리 중 오류: reservationId={}, error={}", r.getId(), e.getMessage());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireReservation(int reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다."));

        if (r.getStatus() != ReservationStatus.PENDING) {
            return; // 이미 처리됨
        }

        r.cancel();
        reservationRepository.save(r);

        // Redis 락 해제 (트랜잭션과 무관하게 실행)
        try {
            seatLockRepository.unlock(r.getConcertId(), r.getSeatId());
        } catch (Exception e) {
            log.warn("좌석 잠금 해제 실패(만료): concert={}, seat={}, error={}", r.getConcertId(), r.getSeatId(), e.getMessage());
        }

        log.info("만료로 예약 취소 처리 완료: reservationId={}, userId={}", r.getId(), r.getUserId());
    }
}
