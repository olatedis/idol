package com.bit.idol.boardservice.dto;

import lombok.Data;

@Data
public class InternalUserResponse {
    private int userId;
    private String nickname;
    private String imgUrl;
    private String role;
    private String status;
}
