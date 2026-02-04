package com.bit.idol.searchservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostSearchResponse {
    private Long postId;
    private String boardType;

    private Long idolId;
    private Long groupId;

    private String title;
    private LocalDateTime createdAt;
}
