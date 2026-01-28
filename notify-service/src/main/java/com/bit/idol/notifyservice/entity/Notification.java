package com.bit.idol.notifyservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * [현재 적용: fanout 이후 USER 단위 저장 모델]
 * - notify-service는 USER 단위 알림만 저장/조회한다.
 * - fanout-service가 대상(userId)을 확정해서 내려준다.
 * - notify DB에는 receiverId로 "내 알림"을 구분한다.
 */
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "notification",
        uniqueConstraints = {
                // 같은 eventId는 중복 저장되면 안 되므로 유니크 권장
                // fanout 이후 eventId는 "원본UUID:userId" 형태라 유니크 충돌 방지 가능
                @UniqueConstraint(name = "uk_notification_event_id", columnNames = {"event_id"})
        },
        indexes = {
                // 유저별 목록 조회 핵심 인덱스
                @Index(name = "idx_notification_receiver_occurred", columnList = "receiver_id, occurred_at"),
                @Index(name = "idx_notification_receiver_id", columnList = "receiver_id, notification_id"),
                @Index(name = "idx_notification_occurred_at", columnList = "occurred_at")
        }
)
public class Notification {

    // DB 내부 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", nullable = false)
    private int notificationId;

    // 수신자 유저 ID (fanout이 USER 단위로 확정해서 내려줌)
    @Column(name = "receiver_id", nullable = false)
    private int receiverId;

    // 예: "VOTE_COMPLETED", "RANKING_UP", "GENERAL_NOTICE"
    @Column(name = "type", nullable = false, length = 80)
    private String type;

    // 예: { "idolName": "아이유", "voteCount": "1", "foodName": "치킨" }
    @Column(name = "args_json", columnDefinition = "JSON")
    private String argsJson;

    // 예: "/votes/123", "/artist/50"
    @Column(name = "redirect_url", nullable = false, length = 255)
    private String redirectUrl;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    // 이벤트 고유 ID(UUID 문자열)
    // fanout 이후: "원본UUID:userId"
    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    // 읽음 처리 시간(미읽음이면 null)
    @Column(name = "read_at")
    private LocalDateTime readAt;

    public static Notification create() {
        return new Notification();
    }

    public void setArgsFromMap(java.util.Map<String, String> args, String jsonString) {
        this.argsJson = jsonString;
    }
}
