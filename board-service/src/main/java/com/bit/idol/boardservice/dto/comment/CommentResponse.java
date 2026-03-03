package com.bit.idol.boardservice.dto.comment;

import lombok.Data;

@Data
public class CommentResponse {

    private Long commentId;
    private Integer authorId;
    private String authorNickname; // 추가됨

    private String content;
    private Boolean isDeleted;

    private String createdAt;
    private String updatedAt;
}
