package com.bit.idol.boardservice.dto;

import com.bit.idol.boardservice.entity.BoardType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostListResponse {
    private Long postId;
    private BoardType boardType;
    private Long idolId;
    private Long groupId;

    private Integer authorId;
    private String title;

    private Integer viewCount;
    private Integer likeCount;
    private Integer dislikeCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
