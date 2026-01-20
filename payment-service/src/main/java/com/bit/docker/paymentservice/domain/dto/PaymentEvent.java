package com.bit.docker.paymentservice.domain.dto;

import com.bit.docker.paymentservice.domain.enumtype.PaymentDomain;
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
}

