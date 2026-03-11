package com.bit.idol.notifyservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notification_preference")
public class NotificationPreference {

    @Id
    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "all_enabled", nullable = false)
    private boolean allEnabled = true;

    @Column(name = "chat_enabled", nullable = false)
    private boolean chatEnabled = true;

    @Column(name = "vote_enabled", nullable = false)
    private boolean voteEnabled = true;

    @Column(name = "ticket_enabled", nullable = false)
    private boolean ticketEnabled = true;

    @Column(name = "board_enabled", nullable = false)
    private boolean boardEnabled = true;

    public static NotificationPreference create(int userId) {
        NotificationPreference p = new NotificationPreference();
        p.userId = userId;
        return p;
    }
}
