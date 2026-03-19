package com.bit.reserveservice.infra.kafka;

import com.bit.reserveservice.domain.dto.PaymentEvent;
import com.bit.reserveservice.domain.dto.ReservationEvent;
import com.bit.reserveservice.domain.entity.Reservation;
import com.bit.reserveservice.domain.enumtype.ReservationStatus;
import com.bit.reserveservice.infra.repository.ReservationRepository;
import com.bit.reserveservice.infra.redis.SeatLockRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@AllArgsConstructor
@Slf4j
public class ReservationEventProducer {

    private static final String NOTIFY_TOPIC = "notify-request-topic";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ReservationRepository reservationRepository;
    private final SeatLockRepository seatLockRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @KafkaListener(topics = "payment.completed", groupId = "reservation-service")
    public void consume(String message) {
        log.info("Kafka 메시지 수신: {}", message);
        try {
            PaymentEvent event = PaymentEvent.fromJson(message);

            // domain이 CONCERT(또는 레거시 RESERVATION/RESERVATE)인 경우만 처리
            String domain = event.getDomain();
            if ("RESERVATE".equalsIgnoreCase(domain) || "RESERVATION".equalsIgnoreCase(domain)) {
                domain = "CONCERT";
            }

            if (!"CONCERT".equals(domain)) {
                log.info("미지원 도메인 필터링: domain={}", event.getDomain());
                return;
            }

            // reservationIds가 없으면 처리 불가
            if (event.getReservationIds() == null || event.getReservationIds().isEmpty()) {
                log.warn("예약 ID가 없음: orderId={}, userId={}", event.getOrderId(), event.getUserId());
                return;
            }

            // seatIds 수집
            List<Integer> seatIds = new ArrayList<>();
            for (Integer reservationId : event.getReservationIds()) {
                Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
                if (reservation != null) {
                    seatIds.add(reservation.getSeatId());
                }
            }

            // seatIds가 포함된 PaymentEvent 재발행 (concert-service용)
            PaymentEvent eventWithSeats = new PaymentEvent(
                    event.getUserId(),
                    event.getOrderId(),
                    domain,
                    event.getTargetId(),
                    event.getAmount(),
                    event.getAgencyId(), // agencyId 전달
                    event.getReservationIds(),
                    seatIds);
            kafkaTemplate.send("payment.completed.with.seats", eventWithSeats.toJson());

            // 각 예약의 상태를 PENDING에서 COMPLETED로 변경
            for (Integer reservationId : event.getReservationIds()) {
                try {
                    Reservation reservation = reservationRepository.findById(reservationId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    String.format("예약을 찾을 수 없음: reservationId=%d", reservationId)));

                    // 예약 상태 확인
                    if (reservation.getStatus() == ReservationStatus.COMPLETED) {
                        log.info("이미 확정된 예약, 재처리 생략: reservationId={}", reservationId);
                        continue;
                    }

                    if (reservation.getStatus() != ReservationStatus.PENDING) {
                        log.warn("예약 상태 불일치 (처리 불가): reservationId={}, status={}",
                                reservationId, reservation.getStatus());
                        continue;
                    }

                    // COMPLETED로 변경
                    reservation.confirm();
                    reservationRepository.save(reservation);

                    // 좌석 잠금 해제
                    try {
                        seatLockRepository.unlock(reservation.getConcertId(), reservation.getSeatId());
                    } catch (Exception e) {
                        log.warn("좌석 잠금 해제 실패: concert={}, seat={}, error={}",
                                reservation.getConcertId(), reservation.getSeatId(), e.getMessage());
                    }

                    log.info("예약 확정 완료: reservationId={}, userId={}, concertId={}, seatId={}",
                            reservationId, reservation.getUserId(),
                            reservation.getConcertId(), reservation.getSeatId());

                } catch (Exception e) {
                    log.error("예약 처리 실패: reservationId={}, error={}", reservationId, e.getMessage());
                }
            }

            // 모든 예약이 처리되면 알림 발행
            boolean allProcessed = true;
            for (Integer reservationId : event.getReservationIds()) {
                Reservation res = reservationRepository.findById(reservationId).orElse(null);
                if (res == null || res.getStatus() != ReservationStatus.COMPLETED) {
                    allProcessed = false;
                    break;
                }
            }

            if (allProcessed && !event.getReservationIds().isEmpty()) {
                reservationRepository.findById(event.getReservationIds().get(0))
                        .ifPresent(this::publishReservationCreated);
            }

        } catch (Exception e) {
            log.error("Kafka 메시지 처리 실패: error={}", e.getMessage(), e);
        }
    }

    public void publishReservationCreated(Reservation reservation) {
        try {
            String uuid = UUID.randomUUID().toString();

            Map<String, String> args = new HashMap<>();
            args.put("concertId", String.valueOf(reservation.getConcertId()));
            args.put("seatId", String.valueOf(reservation.getSeatId()));

            ReservationEvent payload = ReservationEvent.builder()
                    .eventId(uuid)
                    .type("RESERVATION_CREATED")
                    .targetType(ReservationEvent.TargetType.USER)
                    .targetId(String.valueOf(reservation.getUserId()))
                    .args(args)
                    .redirectUrl("/reservation") // TODO: 라우팅 조정
                    .occurredAt(LocalDateTime.now())
                    .build();

            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(NOTIFY_TOPIC, json);

            log.info("예약 알림 발행 성공: type=RESERVATION_CREATED, userId={}, concertId={}",
                    reservation.getUserId(), reservation.getConcertId());
        } catch (Exception e) {
            log.error("예약 알림 발행 실패: userId={}, concertId={}, err={}",
                    reservation.getUserId(), reservation.getConcertId(), e.getMessage());
        }
    }

    public void publishPaymentRequested(PaymentEvent event) {
        kafkaTemplate.send("payment.requested", event.toJson());
        log.info("결제 요청 발행: domain={}, userId={}, targetId={}",
                event.getDomain(), event.getUserId(), event.getTargetId());
    }

}
