package com.bit.subscriptionservice.entity;

public enum SubscriptionStatus {
    PENDING,        // 결제 대기
    ACTIVE,        // 결제 완료, 구독 중
    CANCELED,      // 사용자 해지
    EXPIRED        // 기간 만료
}

