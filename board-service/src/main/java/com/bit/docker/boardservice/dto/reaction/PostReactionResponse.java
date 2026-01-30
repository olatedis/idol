package com.bit.docker.boardservice.dto.reaction;

import lombok.Data;

@Data
public class PostReactionResponse {
    private Integer likeCount;
    private Integer dislikeCount;

    // NONE / LIKE / DISLIKE 중 하나
    private String myReaction;
}
