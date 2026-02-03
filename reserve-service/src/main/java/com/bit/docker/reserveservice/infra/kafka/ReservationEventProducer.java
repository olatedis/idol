package com.bit.docker.reserveservice.infra.kafka;

import com.bit.docker.reserveservice.domain.dto.PaymentEvent;
import com.bit.docker.reserveservice.domain.dto.ReservationEvent;
import com.bit.docker.reserveservice.domain.entity.Reservation;
import com.bit.docker.reserveservice.domain.enumtype.ReservationStatus;
import com.bit.docker.reserveservice.infra.repository.ReservationRepository;
import com.bit.docker.reserveservice.infra.redis.SeatLockRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
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


    @KafkaListener(
            topics = "payment.completed",
            groupId = "reservation-service"
    )

    public void consume(String message) {
        PaymentEvent event = PaymentEvent.fromJson(message);

        if (!"RESERVATION".equals(event.getDomain())) {
            return;
        }

        Reservation reservation =
                reservationRepository
                        .findByUserIdAndSeatIdAndStatus(
                                event.getUserId(),
                                event.getTargetId(),
                                ReservationStatus.PENDING
                        )
                        .orElseThrow();

        reservation.confirm();

        // 예약 성공 후 잠금 해제
        try {
            seatLockRepository.unlock(reservation.getConcertId(), reservation.getSeatId());
        } catch (Exception e) {
            log.warn("좌석 잠금 해제 실패: concert={}, seat={}, error={}", reservation.getConcertId(), reservation.getSeatId(), e.getMessage());
        }

        // 알림 발행
        publishReservationCreated(reservation);

        log.info("좌석 결제 완료: userId={}, concert={}, seat={}",
                reservation.getUserId(), reservation.getConcertId(), reservation.getSeatId());

        /*
        String uuid = UUID.randomUUID().toString();
        Map<String,String> map = new HashMap<>();
        map.put("userId", String.valueOf(event.getUserId()));
        map.put("concertId", String.valueOf(reservation.getConcertId()));
        map.put("seatId", String.valueOf(reservation.getSeatId()));
        kafkaTemplate.send(
                "RESERVATION_CREATED",
                ReservationEvent.builder()
                        .eventId(uuid)
                        .targetType(ReservationEvent.TargetType.USER)
                        .targetId(String.valueOf(reservation.getUserId()))
                        .args(map)
                        .occurredAt(LocalDateTime.now())
                        .build()
                        .toJson()
        );
        */
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
