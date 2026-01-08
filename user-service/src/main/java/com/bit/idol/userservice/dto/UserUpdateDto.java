package com.bit.idol.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDto {
    private String nickname;
    private String email;
    private String phone;
    private String address;
    private String imgUrl;
}
