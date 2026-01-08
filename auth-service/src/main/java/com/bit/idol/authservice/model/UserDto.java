package com.bit.idol.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private int userId;
    private String username;
    private String password;
    private String nickname;
    private Role role;
}
