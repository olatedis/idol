package com.bit.subscriptionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 빌링키 발급 요청 DTO (Toss Payments에서 authKey 발급 후 사용)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingKeyRequest {
    // Toss 결제창에서 받은 일회성 authKey
    private String authKey;
    // 고객 식별자 (UUID 권장)
    private String customerKey;
}
