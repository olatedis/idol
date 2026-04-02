package com.bit.paymentservice.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BillingCompleteRequest {
    private String orderId;
    private String paymentKey;
    private int amount;
}
