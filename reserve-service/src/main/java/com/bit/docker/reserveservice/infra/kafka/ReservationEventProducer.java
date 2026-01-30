package com.bit.docker.reserveservice.infra.kafka;

import com.bit.docker.reserveservice.domain.dto.PaymentEvent;
import com.bit.docker.reserveservice.domain.dto.ReservationEvent;
import com.bit.docker.reserveservice.domain.entity.Reservation;
import com.bit.docker.reserveservice.domain.enumtype.ReservationStatus;
import com.bit.docker.reserveservice.infra.repository.ReservationRepository;
import com.bit.docker.reserveservice.infra.redis.SeatLockRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
@Slf4j
public class ReservationEventProducer {

        private final KafkaTemplate<String, String> kafkaTemplate;
        private final ReservationRepository reservationRepository;
        private final SeatLockRepository seatLockRepository;


    public void publishReservationCreated(PaymentEvent event) {
        kafkaTemplate.send("reservation-created", event.toJson());
    }

        public void publishReservationCanceled(Reservation reservation) {
                kafkaTemplate.send(
                                "notify-request-topic",
                                ReservationEvent.builder()
                                                .eventType("CANCELED")
                                                .targetType(ReservationEvent.TargetType.USER)
                                                .userId(reservation.getUserId())
                                                .concertId(reservation.getConcertId())
                                                .occurredAt(LocalDateTime.now())
                                                .build()
                                                .toJson()
                );
                log.info("좌석 예약 취소 발행: userId={}, concert={}, seat={}", reservation.getUserId(), reservation.getConcertId(), reservation.getSeatId());
        }

    @KafkaListener(
            topics = "payment-completed",
            groupId = "reservation-service"
    )
    public void consume(String message) {

        PaymentEvent event =
                PaymentEvent.fromJson(message);

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

                kafkaTemplate.send(
                "notify-request-topic",
                ReservationEvent.builder()
                        .eventType("CREATED")
                        .targetType(ReservationEvent.TargetType.USER)
                        .userId(reservation.getUserId())
                        .concertId(reservation.getConcertId())
                        .occurredAt(LocalDateTime.now())
                        .build()
                        .toJson()
        );

        log.info("좌석 결제 완료: userId={}, concert={}, seat={}", reservation.getUserId(), reservation.getConcertId(), reservation.getSeatId());


    }

}
