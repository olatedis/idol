package com.bit.docker.reserveservice.infra.kafka;

import com.bit.docker.reserveservice.domain.dto.PaymentEvent;
import com.bit.docker.reserveservice.domain.dto.ReservationEvent;
import com.bit.docker.reserveservice.domain.entity.Reservation;
import com.bit.docker.reserveservice.domain.enumtype.ReservationStatus;
import com.bit.docker.reserveservice.infra.repository.ReservationRepository;
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


    public void publishReservationCreated(PaymentEvent event) {
        kafkaTemplate.send("reservation-created", event.toJson());
    }

    @KafkaListener(
            topics = "payment.completed",
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
