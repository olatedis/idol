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

    @jakarta.validation.constraints.NotBlank(message = "사용자 이름은 필수입니다.")
    private String username;

    @jakarta.validation.constraints.NotBlank(message = "비밀번호는 필수입니다.")
    @jakarta.validation.constraints.Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @jakarta.validation.constraints.NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    @jakarta.validation.constraints.NotBlank(message = "이메일은 필수입니다.")
    @jakarta.validation.constraints.Email(message = "유효하지 않은 이메일 형식입니다.")
    private String email;

    @jakarta.validation.constraints.NotBlank(message = "전화번호는 필수입니다.")
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
