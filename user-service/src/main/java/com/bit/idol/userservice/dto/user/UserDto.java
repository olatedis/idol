package com.bit.idol.userservice.dto.user;

import com.bit.idol.userservice.entity.Role;
import com.bit.idol.userservice.entity.User;
import com.bit.idol.userservice.entity.UserStatus;
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

    private String password;

    @jakarta.validation.constraints.NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    @jakarta.validation.constraints.NotBlank(message = "이메일은 필수입니다.")
    @jakarta.validation.constraints.Email(message = "유효하지 않은 이메일 형식입니다.")
    private String email;

    private String phone;

    private String address;
    private Role role;
    private String imgUrl;
    
    private String provider;
    private String providerId;

    // 상태 필드 추가
    private UserStatus status;

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
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .status(user.getStatus()) // 상태값 매핑
                .build();
    }
}
