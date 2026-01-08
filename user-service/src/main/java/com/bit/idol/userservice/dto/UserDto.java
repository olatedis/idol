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
public class UserDto {
    private int userId;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String address;
    private Role role;
    private String imgUrl;

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .imgUrl(user.getImgUrl())
                .build();
    }
}
