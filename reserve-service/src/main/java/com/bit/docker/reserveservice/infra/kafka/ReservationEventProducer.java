package com.bit.docker.reserveservice.infra.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReservationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ReservationEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishReservationCreated(Long reservationId) {
        kafkaTemplate.send("reservation-created", reservationId);
    }
}
