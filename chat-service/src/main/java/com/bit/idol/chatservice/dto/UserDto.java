package com.bit.idol.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private int userId;
    private String username;
    private String nickname;
    private String role; // USER or IDOL
    private String imgUrl; // 프로필 이미지 URL 추가
}
