package com.bit.docker.boardservice.controller;

import com.bit.docker.boardservice.dto.PostListResponse;
import com.bit.docker.boardservice.dto.PostResponse;
import com.bit.docker.boardservice.entity.BoardType;
import com.bit.docker.boardservice.service.PostService;
import com.bit.docker.boardservice.service.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notices")
public class NoticeController {

    private final PostService postService;

    // 공지 목록 (전체 공개)
    @GetMapping
    public Page<PostListResponse> showNoticeList(Pageable pageable) {
        return postService.selectAll(BoardType.ADMIN_NOTICE, null, null, pageable);
    }

    // 공지 상세 (전체 공개)
    @GetMapping("/{postId}")
    public PostResponse showNotice(@PathVariable Long postId) {
        // 공지는 구독/권한 체크 없음 → 더미 값으로 통과
        return postService.selectOne(postId, -1, Role.USER);
    }
}
