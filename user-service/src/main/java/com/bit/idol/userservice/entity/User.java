package com.bit.idol.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
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

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String email;

    private String phone;

    private String address;

    private String imgUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // --- 소셜 로그인 필드 추가 ---
    private String provider;   // KAKAO, GOOGLE, NAVER
    private String providerId; // 소셜 서비스의 고유 ID
}
