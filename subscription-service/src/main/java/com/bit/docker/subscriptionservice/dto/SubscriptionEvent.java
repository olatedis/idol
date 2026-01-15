package com.bit.docker.subscriptionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionEvent {

    private String eventType; // CREATED, CANCELED, EXPIRED
    private int userId;
    private Long idolId;
    private LocalDateTime occurredAt;
}

