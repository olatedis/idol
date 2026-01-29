package com.bit.idol.notifyservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 아이돌 메시지 알림 "스택형" 카운트
 * - (receiverId, idolId) 단위로 unreadCount 누적
 * - lastOccurredAt 기준으로 "최근 온 아이돌" 정렬에 사용
 * - type은 넣지 않음(이번 기능 범위: IDOL_MESSAGE 전용)
 */
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "idol_message_stack",
        uniqueConstraints = {
                // 유저-아이돌 조합은 1행만 유지(메시지가 하나 더 왔다고 새 row를 추가하지 않는다는 뜻)
                @UniqueConstraint(name = "uk_stack_receiver_idol", columnNames = {"receiver_id", "idol_id"})
        },
        indexes = {
                // 유저별 목록 조회 + 최신 정렬
                @Index(name = "idx_stack_receiver_last", columnList = "receiver_id, last_occurred_at")
        }
)
public class IdolMessageStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stack_id", nullable = false)
    private long stackId;

    @Column(name = "receiver_id", nullable = false)
    private int receiverId;

    @Column(name = "idol_id", nullable = false)
    private long idolId;

    @Column(name = "unread_count", nullable = false)
    private int unreadCount;

    @Column(name = "last_occurred_at", nullable = false)
    private LocalDateTime lastOccurredAt;

    public static IdolMessageStack create(int receiverId, long idolId, LocalDateTime occurredAt) {
        IdolMessageStack s = new IdolMessageStack();
        s.receiverId = receiverId;
        s.idolId = idolId;
        s.unreadCount = 1;
        s.lastOccurredAt = occurredAt;
        return s;
    }

    public void increment(LocalDateTime occurredAt) {
        this.unreadCount += 1;
        this.lastOccurredAt = occurredAt;
    }

    public void reset(LocalDateTime occurredAt) {
        this.unreadCount = 0;
        this.lastOccurredAt = occurredAt;
    }
}
