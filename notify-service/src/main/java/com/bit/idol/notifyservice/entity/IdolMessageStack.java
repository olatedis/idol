package com.bit.idol.notifyservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 아이돌 메시지 스택(카톡 뱃지형)
 * - (receiverId, idolId) 조합으로 1행만 유지
 * - unreadCount 증가 + lastOccurredAt 최신값 유지
 * - reset 시 unreadCount=0만 변경, lastOccurredAt은 유지
 */
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "idol_message_stack",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stack_receiver_idol",
                        columnNames = {"receiver_id", "idol_id"}
                )
        },
        indexes = {
                @Index(name = "idx_stack_receiver_last", columnList = "receiver_id, last_occurred_at"),
                @Index(name = "idx_stack_receiver", columnList = "receiver_id")
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

    public static IdolMessageStack create() {
        return new IdolMessageStack();
    }
}
