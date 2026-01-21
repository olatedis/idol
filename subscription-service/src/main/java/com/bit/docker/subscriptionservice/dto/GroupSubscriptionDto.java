package com.bit.docker.subscriptionservice.dto;

import com.bit.docker.subscriptionservice.entity.GroupSubscription;
import com.bit.docker.subscriptionservice.entity.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupSubscriptionDto {

    // 그룹 구독 DTO
    private Long subscriptionId;
    private int userId;
    private Long groupId;
    private SubscriptionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime expiredAt;
    private boolean autoRenew;

    public static GroupSubscriptionDto fromEntity(GroupSubscription gs) {
        return GroupSubscriptionDto.builder()
                .subscriptionId(gs.getId())
                .userId(gs.getUserId())
                .groupId(gs.getGroupId())
                .status(gs.getStatus())
                .startedAt(gs.getStartedAt())
                .expiredAt(gs.getExpiredAt())
                .autoRenew(gs.isAutoRenew())
                .build();
    }
}
