package com.bit.idol.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDto {
    private String nickname;

    @jakarta.validation.constraints.Email(message = "유효하지 않은 이메일 형식입니다.")
    private String email;
    private String phone;
    private String address;
    private String imgUrl;
}
