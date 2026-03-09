package com.bit.idol.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ban_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BanHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int userId; // 제재 대상자 ID

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private UserStatus status; // 변경된 상태 (SUSPENDED, BANNED, ACTIVE, RESTRICTED)

    private String reason; // 제재/해제 사유

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
