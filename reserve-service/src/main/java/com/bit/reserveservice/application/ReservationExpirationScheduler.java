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

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationExpirationScheduler {

    private final ReservationRepository reservationRepository;
    private final ReservationExpirationHandler expirationHandler;

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
                // 별도 클래스의 메서드 호출로 트랜잭션 프록시 적용
                expirationHandler.expireReservation(r.getId());
            } catch (Exception e) {
                log.error("예약 만료 처리 중 오류: reservationId={}, error={}", r.getId(), e.getMessage());
            }
        }
    }
}
