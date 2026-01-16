package com.bit.idol.notifyservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "notification",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_receiver_event",
                        columnNames = {"receiver_id", "event_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_notification_receiver_created",
                        columnList = "receiver_id, created_at"
                ),
                @Index(
                        name = "idx_notification_receiver_unread",
                        columnList = "receiver_id, read_at, created_at"
                )
        }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", nullable = false)
    private int notificationId;

    @Column(name = "receiver_id", nullable = false)
    private int receiverId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "category",
            nullable = false,
            columnDefinition = "ENUM('CHAT','VOTE','TICKET','NOTICE','SYSTEM')"
    )
    private NotificationType category;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "body", nullable = false, length = 500)
    private String body;

    @Column(name = "deeplink", nullable = false, length = 255)
    private String deeplink;

    /*
    @Enumerated(EnumType.STRING)
    @Column(
            name = "ref_type",
            nullable = false,
            columnDefinition = "ENUM('CHAT_ROOM','VOTE','TICKET','NOTICE')"
    )
    private NotificationRefType refType;

    @Column(name = "ref_id", nullable = false)
    private int refId;
    */

    @Column(name = "attributes_json", columnDefinition = "JSON")
    private String attributesJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public static Notification create() {
        return new Notification();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
