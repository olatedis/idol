package com.bit.subscriptionservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "group_subscriptions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "group_id"})
        },
        indexes = {
                @Index(name = "idx_gsub_user_id", columnList = "user_id"),
                @Index(name = "idx_gsub_group_id", columnList = "group_id"),
                @Index(name = "idx_gsub_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 0121 그룹id관련 생성
public class GroupSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "group_id", nullable = false)
    private int groupId;

    @Column(nullable = false)
    private String groupName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime expiredAt;

    @Column(nullable = false)
    private boolean autoRenew;

    public void cancel() {
        this.status = SubscriptionStatus.CANCELED;
        this.expiredAt = LocalDateTime.now();
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
        this.expiredAt = LocalDateTime.now();
    }
}
