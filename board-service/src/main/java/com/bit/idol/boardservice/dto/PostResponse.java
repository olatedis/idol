package com.bit.idol.boardservice.dto;

import com.bit.idol.boardservice.dto.comment.CommentResponse;
import com.bit.idol.boardservice.entity.BoardType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostResponse {

    private Long postId;
    private BoardType boardType;
    private Long idolId;
    private Long groupId;

    private Integer authorId;
    private String authorNickname; // 추가
    private String title;
    private String content;

    private Integer viewCount;
    private Integer likeCount;
    private Integer dislikeCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<CommentResponse> comments;

}
