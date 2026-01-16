package com.bit.idol.userservice.dto.user;

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

    // 소셜 로그인 시 비밀번호는 null일 수 있으므로 유효성 검사 조건부 적용 필요 (여기선 일단 유지)
    private String password;

    @jakarta.validation.constraints.NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    @jakarta.validation.constraints.NotBlank(message = "이메일은 필수입니다.")
    @jakarta.validation.constraints.Email(message = "유효하지 않은 이메일 형식입니다.")
    private String email;

    private String phone; // 소셜 로그인 시 전화번호 없을 수 있음 (필수 해제)

    private String address;
    private Role role;
    private String imgUrl;
    
    // 소셜 필드 추가
    private String provider;
    private String providerId;

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
                .build();
    }
}
