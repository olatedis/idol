package com.bit.docker.paymentservice.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmDto {

    private String paymentKey;
    private String orderId;
    private int amount;
}
