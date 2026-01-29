package com.bit.docker.subscriptionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 빌링키 응답 DTO (Toss Payments API 응답)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingKeyResponse {
    private String mId;
    private String customerKey;
    private String authenticatedAt;
    private String method;
    private String billingKey;
    private CardInfo card;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CardInfo {
        private String issuerCode;
        private String acquirerCode;
        private String number;
        private String cardType;
        private String ownerType;
    }
}
