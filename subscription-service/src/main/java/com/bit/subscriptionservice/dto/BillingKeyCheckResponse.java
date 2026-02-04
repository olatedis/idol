package com.bit.subscriptionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 빌링키 존재 여부 확인 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BillingKeyCheckResponse {
    
    private boolean hasBillingKey;
}
