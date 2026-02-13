package com.bit.subscriptionservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "subscriptions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "idol_id"})
        },
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_idol_id", columnList = "idol_id"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_idol_stage_name", columnList = "stage_name")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "idol_id", nullable = false)
    private int idolId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime expiredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan plan;

    private LocalDateTime nextRenewalAt;  // 다음 갱신 예정 시간

    @Column(nullable = false)
    private boolean autoRenew;

    public void activate() {
        if (this.status != SubscriptionStatus.PENDING) {
            throw new IllegalStateException("활성화 불가 상태");
        }
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
        this.nextRenewalAt = LocalDateTime.now().plusMonths(plan.getDurationInMonths());
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELED;
        this.expiredAt = LocalDateTime.now();
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
        this.expiredAt = LocalDateTime.now();
    }

    public void renew() {
        if (this.status != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("활성 구독만 갱신 가능");
        }
        this.startedAt = LocalDateTime.now();
        this.nextRenewalAt = LocalDateTime.now().plusMonths(plan.getDurationInMonths());
    }

    /**
     * 구독이 활성 상태인지 확인
     */
    public boolean isActive() {
        return this.status == SubscriptionStatus.ACTIVE;
    }

    /**
     * 구독이 유효한 상태인지 확인 (활성 또는 대기 중)
     */
    public boolean isValid() {
        return this.status == SubscriptionStatus.ACTIVE || this.status == SubscriptionStatus.PENDING;
    }
}
