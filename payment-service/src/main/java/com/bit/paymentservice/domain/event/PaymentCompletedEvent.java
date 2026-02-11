package com.bit.paymentservice.domain.event;

import com.bit.paymentservice.domain.dto.PaymentEvent;

public record PaymentCompletedEvent(PaymentEvent event) {
}
