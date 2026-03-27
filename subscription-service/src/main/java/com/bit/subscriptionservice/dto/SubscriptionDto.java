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
    private String idolStageName; //구독중 아이돌 활동명 : 내 구독 아이돌 출력시 사용
    private SubscriptionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime expiredAt;
    private String orderId;    // 결제 주문번호 추가
    private int amount;        // 결제 금액 추가
    private boolean autoRenew;

    public static SubscriptionDto fromEntity(Subscription subscription) {
        return fromEntity(subscription, null, 0);
    }

    public static SubscriptionDto fromEntity(Subscription subscription, String orderId, int amount) {
        return SubscriptionDto.builder()
                .subscriptionId(subscription.getId())
                .userId(subscription.getUserId())
                .idolId(subscription.getIdolId())
                .idolStageName(subscription.getIdolStageName())
                .status(subscription.getStatus())
                .startedAt(subscription.getStartedAt())
                .expiredAt(subscription.getExpiredAt())
                .orderId(orderId)
                .amount(amount)
                .autoRenew(subscription.isAutoRenew())
                .build();
    }
}
