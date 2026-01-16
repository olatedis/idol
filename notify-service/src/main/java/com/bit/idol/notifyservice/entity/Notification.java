package com.bit.idol.notifyservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 새 Notification 모델(요청형):
 * - 기존 receiverId/title/body/deeplink/refType/refId/readAt 등은 폐기
 * - eventId(UUID), type(템플릿 키), targetType/targetId, args, redirectUrl, occurredAt 저장
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "notification",
        uniqueConstraints = {
                // 같은 eventId는 중복 저장되면 안 되므로 유니크 권장
                @UniqueConstraint(name = "uk_notification_event_id", columnNames = {"event_id"})
        },
        indexes = {
                // target 기반 조회를 많이 할 가능성이 큼
                @Index(name = "idx_notification_target", columnList = "target_type, target_id"),
                @Index(name = "idx_notification_occurred_at", columnList = "occurred_at")
        }
)
public class Notification {


    // DB 내부 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", nullable = false)
    private int notificationId;

    // 예: "VOTE_COMPLETED", "RANKING_UP", "GENERAL_NOTICE"
    @Column(name = "type", nullable = false, length = 80)
    private String type;

    // 예: USER(특정인), IDOL_SUB(아이돌구독자), GROUP_SUB(그룹구독자), ALL(전체)
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private TargetType targetType;

    // 예: "100"(유저ID), "50"(아이돌ID), "10"(그룹ID), null(전체)
    @Column(name = "target_id", length = 80)
    private String targetId;

    // 예: { "idolName": "아이유", "voteCount": "1", "foodName": "치킨" }
    @Column(name = "args_json", columnDefinition = "JSON")
    private String argsJson;

    // 예: "/votes/123", "/artist/50"
    @Column(name = "redirect_url", nullable = false, length = 255)
    private String redirectUrl;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    // 이벤트 고유 ID(UUID 문자열)
    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    public static Notification create() {
        return new Notification();
    }


    public void setArgsFromMap(Map<String, String> args, String jsonString) {
        this.argsJson = jsonString;
    }
}
