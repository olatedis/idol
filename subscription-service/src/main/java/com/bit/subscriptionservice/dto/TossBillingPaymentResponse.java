package com.bit.subscriptionservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossBillingPaymentResponse {
    private String paymentKey;
    private String orderId;
    private String status;
    private int totalAmount;
}
