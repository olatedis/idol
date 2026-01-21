package com.bit.docker.paymentservice.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmDto {

    private String paymentKey;
    private String orderId;
    private int amount;
}
