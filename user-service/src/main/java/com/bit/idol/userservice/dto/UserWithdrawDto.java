package com.bit.idol.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserWithdrawDto {
    @jakarta.validation.constraints.NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}
