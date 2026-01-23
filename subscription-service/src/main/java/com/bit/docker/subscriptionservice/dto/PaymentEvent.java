package com.bit.docker.subscriptionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentEvent {
    private int userId;
    private String orderId;
    private PaymentDomain domain;
    private int targetId;
    private int amount;

    public enum PaymentDomain {
        CONCERT,
        SUBSCRIPTION
    }
}

