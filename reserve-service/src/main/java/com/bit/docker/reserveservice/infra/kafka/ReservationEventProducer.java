package com.bit.docker.reserveservice.infra.kafka;

import com.bit.docker.reserveservice.domain.dto.PaymentEvent;
import lombok.AllArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ReservationEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;


    public void publishReservationCreated(PaymentEvent event) {
        kafkaTemplate.send("reservation-created", event.toJson());
    }
}
