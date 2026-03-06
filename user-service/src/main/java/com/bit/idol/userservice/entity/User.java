package com.bit.idol.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_nickname", columnList = "nickname"),
        @Index(name = "idx_provider", columnList = "provider, providerId") // 소셜 로그인 조회용 복합 인덱스
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String username; // 로그인 ID (소셜은 provider_providerId 조합 사용)

    @Column(nullable = false)
    private String password; // 소셜 로그인은 임의의 값 저장

    @Column(nullable = false, unique = true) // 닉네임 중복 방지 추가
    private String nickname;

    private String realName; // 실명 (소셜 이름) - 추가됨

    @Column(nullable = false)
    private String email;

    private String phone;

    private String address;

    private String imgUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // --- 소셜 로그인 필드 ---
    private String provider; // KAKAO, GOOGLE, NAVER
    private String providerId; // 소셜 서비스의 고유 ID

    // --- 신고 및 제재 필드 ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE; // 기본값: 정상

    @Column(nullable = false)
    @Builder.Default
    private int reportCount = 0; // 누적 신고 횟수

    private LocalDateTime suspendedUntil; // 일시 정지/제한 해제 일시

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt; // 가입일 (뉴비 차단용)
}
