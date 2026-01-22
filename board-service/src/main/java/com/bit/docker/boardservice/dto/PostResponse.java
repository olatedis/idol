package com.bit.docker.boardservice.dto;

import lombok.Data;

@Data
public class PostResponse {

    private Long postId;
    private String boardType;
    private Long idolId;
    private Long groupId;

    private Integer authorId;
    private String title;
    private String content;

    private Integer viewCount;
    private Integer likeCount;

    private String createdAt;
    private String updatedAt;

}
