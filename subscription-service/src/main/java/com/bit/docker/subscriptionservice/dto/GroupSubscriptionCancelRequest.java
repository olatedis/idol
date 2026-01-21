package com.bit.docker.subscriptionservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GroupSubscriptionCancelRequest {

    @NotNull
    private Long groupId;
}

