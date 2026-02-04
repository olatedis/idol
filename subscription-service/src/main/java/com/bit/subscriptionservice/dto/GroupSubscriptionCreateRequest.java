package com.bit.subscriptionservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GroupSubscriptionCreateRequest {

    @NotNull
    private int groupId;

    private boolean autoRenew;
}

