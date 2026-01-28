package com.bit.idol.voteservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private int userId;
    private String username;
    private String nickname;
    private String role;
    private LocalDateTime createdAt; // 가입일 (뉴비 차단용)
}
