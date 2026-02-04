package com.bit.concertservice.infra.kafka;

import com.bit.concertservice.domain.dto.ReservationEvent;
import com.bit.concertservice.infra.SeatRepository;
import com.bit.concertservice.domain.entity.Seat;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
public class ConcertReservationConsumer {

    private final SeatRepository seatRepository;

    @KafkaListener(topics = {"notify-request-topic", "RESERVATION_CREATED"}, groupId = "concert-service")
    @Transactional
    public void consume(String message) {
        ReservationEvent event = ReservationEvent.fromJson(message);

        try {
            if ("CREATED".equals(event.getEventType())) {
                Optional<Seat> opt = seatRepository.findById(event.getSeatId());
                if (opt.isPresent()) {
                    Seat seat = opt.get();
                    seat.lock(event.getUserId());
                    seatRepository.save(seat);
                    log.info("좌석 잠금 처리: concertId={}, seatId={}, userId={}", event.getConcertId(), event.getSeatId(), event.getUserId());
                } else {
                    log.warn("잠금할 좌석 없음: concertId={}, seatId={}", event.getConcertId(), event.getSeatId());
                }
            } else if ("CANCELED".equals(event.getEventType()) || "EXPIRED".equals(event.getEventType())) {
                Optional<Seat> opt = seatRepository.findById(event.getSeatId());
                if (opt.isPresent()) {
                    Seat seat = opt.get();
                    seat.unlock();
                    seatRepository.save(seat);
                    log.info("좌석 잠금 해제 처리: concertId={}, seatId={}, userId={}", event.getConcertId(), event.getSeatId(), event.getUserId());
                }
            }
        } catch (Exception e) {
            log.error("ReservationEvent 처리 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
}
