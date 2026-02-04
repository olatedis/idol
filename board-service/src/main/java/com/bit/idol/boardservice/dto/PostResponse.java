package com.bit.idol.boardservice.dto;

import com.bit.idol.boardservice.dto.comment.CommentResponse;
import lombok.Data;

import java.util.List;

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
    private Integer dislikeCount;

    private String createdAt;
    private String updatedAt;

    private List<CommentResponse> comments;

}
