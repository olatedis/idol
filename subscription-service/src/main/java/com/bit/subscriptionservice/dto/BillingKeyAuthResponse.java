package com.bit.subscriptionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 빌링키 발급 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BillingKeyAuthResponse {
    
    private int billingKeyId;
    private String cardNumber;
    private String cardIssuer;
    private String cardType;
    private String message;
}
