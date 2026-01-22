package com.bit.docker.boardservice.dto;

import lombok.Data;

@Data
public class PostUpdateRequest {
    private String title;
    private String content;
}
