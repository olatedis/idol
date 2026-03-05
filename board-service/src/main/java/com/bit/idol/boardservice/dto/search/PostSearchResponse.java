package com.bit.idol.boardservice.dto.search;

import lombok.Data;

@Data
public class PostSearchResponse {
    private Long postId;
    private String boardType;
    private Long idolId;
    private Long groupId;
    private String title;
    private String createdAt;
}