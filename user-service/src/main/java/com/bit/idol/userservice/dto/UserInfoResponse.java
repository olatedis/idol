package com.bit.idol.userservice.dto;

import com.bit.idol.userservice.entity.Role;
import com.bit.idol.userservice.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String address;
    private Role role;
    private String imgUrl;

    public static UserInfoResponse fromEntity(User user) {
        return UserInfoResponse.builder()
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .imgUrl(user.getImgUrl())
                .build();
    }
}
