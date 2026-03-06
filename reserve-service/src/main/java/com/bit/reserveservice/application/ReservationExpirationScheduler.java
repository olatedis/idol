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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
        log.info("예약 만료 체크 시작... (cutoff: {}분 전)", expireMinutes);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(expireMinutes);
        
        // 트랜잭션 없이 조회 (OSIV 껐으므로 안전)
        List<Reservation> expired = reservationRepository.findByStatusAndCreatedAtBefore(ReservationStatus.PENDING, cutoff);
        
        log.info("만료된 예약 조회 결과: {}건 (cutoff: {})", expired.size(), cutoff);
        
        if (expired.isEmpty()) {
            log.debug("만료된 예약 없음");
            return;
        }

        log.info("만료된 예약 {}건 발견. 취소 처리 시작...", expired.size());

        for (Reservation r : expired) {
            try {
                log.info("예약 만료 처리: reservationId={}, userId={}, createdAt={}", 
                        r.getId(), r.getUserId(), r.getCreatedAt());
                // 별도 클래스의 메서드 호출로 트랜잭션 프록시 적용
                expirationHandler.expireReservation(r.getId());
            } catch (Exception e) {
                log.error("예약 만료 처리 중 오류: reservationId={}, error={}", r.getId(), e.getMessage());
            }
        }
        
        log.info("예약 만료 체크 완료");
    }

    // 수동 실행을 위한 메서드 (테스트용)
    public void expirePendingReservationsManual() {
        log.info("수동 예약 만료 체크 실행");
        expirePendingReservations();
    }
}

@RestController
class ReservationExpirationController {
    
    private final ReservationExpirationScheduler scheduler;
    
    ReservationExpirationController(ReservationExpirationScheduler scheduler) {
        this.scheduler = scheduler;
    }
    
    @GetMapping("/admin/expire-reservations")
    public String expireReservations() {
        scheduler.expirePendingReservationsManual();
        return "예약 만료 처리 실행됨";
    }
}
