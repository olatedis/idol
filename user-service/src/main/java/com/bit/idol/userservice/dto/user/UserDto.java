package com.bit.idol.userservice.dto.user;

import com.bit.idol.userservice.entity.Role;
import com.bit.idol.userservice.entity.User;
import com.bit.idol.userservice.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "사용자 이름은 필수입니다.")
    private String username;

    private String password;

    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    private String realName; // 실명 추가

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "유효하지 않은 이메일 형식입니다.")
    private String email;

    private String phone;

    private String address;
    private Role role;
    private String imgUrl;

    private String provider;
    private String providerId;

    private UserStatus status;
    private int reportCount;
    private LocalDateTime suspendedUntil; // 일시 정지 해제 시간

    private LocalDateTime createdAt; // 가입일 추가

    // 이메일 인증 토큰 (회원가입 시 필수)
    private String verificationToken;

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .nickname(user.getNickname())
                .realName(user.getRealName()) // 매핑 추가
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .imgUrl(user.getImgUrl())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .status(user.getStatus())
                .reportCount(user.getReportCount())
                .suspendedUntil(user.getSuspendedUntil())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
