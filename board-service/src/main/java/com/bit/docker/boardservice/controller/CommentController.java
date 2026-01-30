package com.bit.docker.boardservice.controller;

import com.bit.docker.boardservice.dto.comment.CommentResponse;
import com.bit.docker.boardservice.dto.comment.CommentUpdateRequest;
import com.bit.docker.boardservice.dto.comment.CommentWriteRequest;
import com.bit.docker.boardservice.service.CommentService;
import com.bit.docker.boardservice.service.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class CommentController {

    private final CommentService commentService;

    // 댓글작성
    @PostMapping("/{postId}/comments")
    public CommentResponse write(
            @PathVariable Long postId,
            @RequestBody CommentWriteRequest req,
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader("X-User-Role") Role role
    ) {
        return commentService.write(postId, req, userId, role);
    }

    // 댓글 목록 조회
    @GetMapping("/{postId}/comments")
    public List<CommentResponse> showAll(
            @PathVariable Long postId,
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader("X-User-Role") Role role
    ) {
        return commentService.showAll(postId, userId, role);
    }

    // 댓글 수정
    @PutMapping("/comments/{commentId}")
    public CommentResponse update(
            @PathVariable Long commentId,
            @RequestBody CommentUpdateRequest req,
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader("X-User-Role") Role role
    ) {
        return commentService.update(commentId, req, userId, role);
    }

    // 댓글 삭제(소프트)
    @DeleteMapping("/comments/{commentId}")
    public void delete(
            @PathVariable Long commentId,
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader("X-User-Role") Role role
    ) {
        commentService.delete(commentId, userId, role);
    }
}
