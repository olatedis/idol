package com.bit.reserveservice.domain.event;

import com.bit.reserveservice.domain.dto.PaymentEvent;

public record ReservationCreatedEvent(PaymentEvent event) {
}
