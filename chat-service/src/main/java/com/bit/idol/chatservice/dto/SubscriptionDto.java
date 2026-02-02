package com.bit.idol.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto {
    private int subscriptionId;
    private int userId;
    private int idolId;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime expiredAt;
    private boolean autoRenew;
}
