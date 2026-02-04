package com.bit.subscriptionservice.dto;

import com.bit.subscriptionservice.entity.Subscription;
import com.bit.subscriptionservice.entity.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionDto {

    private int subscriptionId;
    private int userId;
    private int idolId;
    private SubscriptionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime expiredAt;
    private boolean autoRenew;

    public static SubscriptionDto fromEntity(Subscription subscription) {
        return SubscriptionDto.builder()
                .subscriptionId(subscription.getId())
                .userId(subscription.getUserId())
                .idolId(subscription.getIdolId())
                .status(subscription.getStatus())
                .startedAt(subscription.getStartedAt())
                .expiredAt(subscription.getExpiredAt())
                .autoRenew(subscription.isAutoRenew())
                .build();
    }
}
