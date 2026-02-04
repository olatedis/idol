package com.bit.idol.boardservice.controller;

import com.bit.idol.boardservice.dto.reaction.PostReactionResponse;
import com.bit.idol.boardservice.service.PostReactionService;
import com.bit.idol.boardservice.service.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostReactionController {

    private final PostReactionService postReactionService;

    // 추천 토글
    @PostMapping("/{postId}/like")
    public PostReactionResponse like(
            @PathVariable Long postId,
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader("X-User-Role") Role role
    ) {
        return postReactionService.like(postId, userId, role);
    }

    // 비추천 토글
    @PostMapping("/{postId}/dislike")
    public PostReactionResponse dislike(
            @PathVariable Long postId,
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader("X-User-Role") Role role
    ) {
        return postReactionService.dislike(postId, userId, role);
    }

    @GetMapping("/{postId}/reaction")
    public PostReactionResponse myReaction(
            @PathVariable Long postId,
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader("X-User-Role") Role role
    ) {
        return postReactionService.getMyReaction(postId, userId, role);
    }
}
