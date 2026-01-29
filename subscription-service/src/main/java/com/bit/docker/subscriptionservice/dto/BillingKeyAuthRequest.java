package com.bit.docker.subscriptionservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 빌링키 발급 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BillingKeyAuthRequest {
    
    @NotNull
    private int idolId;
    
    @NotBlank
    private String authKey;
    
    @NotBlank
    private String plan; // MONTHLY or ANNUAL
}
