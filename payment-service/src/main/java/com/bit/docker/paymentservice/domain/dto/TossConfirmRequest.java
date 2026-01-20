package com.bit.docker.paymentservice.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TossConfirmRequest {
    private String paymentKey;
    private String orderId;
    private int amount;
}

