package com.bit.subscriptionservice.dto;

import com.bit.subscriptionservice.entity.GroupSubscription;
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
public class GroupSubscriptionDto {

    // 그룹 구독 DTO
    private int subscriptionId;
    private int userId;
    private int groupId;
    private String groupName;
    private SubscriptionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime expiredAt;
    private boolean autoRenew;
    private String groupImage;

    public static GroupSubscriptionDto fromEntity(GroupSubscription gs) {
        return GroupSubscriptionDto.builder()
                .subscriptionId(gs.getId())
                .userId(gs.getUserId())
                .groupId(gs.getGroupId())
                .groupName(gs.getGroupName())
                .status(gs.getStatus())
                .startedAt(gs.getStartedAt())
                .expiredAt(gs.getExpiredAt())
                .autoRenew(gs.isAutoRenew())
                .build();
    }

    public void setGroupImage(String groupImage) {
        this.groupImage = groupImage;
    }
}
