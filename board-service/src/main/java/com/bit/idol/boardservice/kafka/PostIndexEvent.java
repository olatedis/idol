package com.bit.idol.boardservice.kafka;

import lombok.Data;

@Data
public class PostIndexEvent {
    // upsert / delete
    private String action;

    private Long postId;

    private String boardType;
    private Long idolId;
    private Long groupId;

    private String title;
    private String content;

    private String createdAt;
    private String updatedAt;

}
