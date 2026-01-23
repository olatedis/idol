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
    private int idolId; // targetType=IDOL일때
    private LocalDateTime occurredAt;

    // 0121 그룹id관련 수정(추가)
    private int groupId;
    private TargetType targetType; // targetType=GROUP일때
    public enum TargetType {
        IDOL, GROUP
    }
}

