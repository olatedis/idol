package com.bit.docker.paymentservice.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentCreateResponse {
    private String orderId;
    private int amount;
}
