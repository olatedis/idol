package com.bit.idol.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeDto {
    @jakarta.validation.constraints.NotBlank(message = "현재 비밀번호는 필수입니다.")
    private String currentPassword;

    @jakarta.validation.constraints.NotBlank(message = "새 비밀번호는 필수입니다.")
    @jakarta.validation.constraints.Size(min = 8, message = "새 비밀번호는 8자 이상이어야 합니다.")
    private String newPassword;
}
