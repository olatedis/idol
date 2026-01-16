package com.bit.idol.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private int userId;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private Role role;
    private String imgUrl;
    
    // 소셜 로그인 정보
    private String provider;
    private String providerId;

    // 유저 상태 (ACTIVE, BANNED, SUSPENDED)
    private String status;
}
