package com.bit.docker.paymentservice.domain.dto;

import lombok.Getter;

@Getter
public class TossConfirmResponse {
    private String paymentKey;
    private String orderId;
    private String status;
    private int totalAmount;
}


