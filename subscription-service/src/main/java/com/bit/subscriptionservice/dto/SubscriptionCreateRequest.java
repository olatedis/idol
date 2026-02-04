package com.bit.subscriptionservice.dto;

import com.bit.subscriptionservice.entity.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionCreateRequest {

    @NotNull
    private int idolId;

    @NotNull
    private SubscriptionPlan plan;  // MONTHLY 또는 ANNUAL

    private boolean autoRenew;
}

