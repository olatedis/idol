package com.bit.idol.boardservice.controller;

import com.bit.idol.boardservice.dto.PostListResponse;
import com.bit.idol.boardservice.dto.PostResponse;
import com.bit.idol.boardservice.dto.PostUpdateRequest;
import com.bit.idol.boardservice.dto.PostWriteRequest;
import com.bit.idol.boardservice.entity.BoardType;
import com.bit.idol.boardservice.service.PostService;
import com.bit.idol.boardservice.service.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final PostService postService;

    // 공지 작성 (ADMIN만)
    @PostMapping("/notices")
    public PostResponse writeNotice(
            @RequestBody PostWriteRequest req,
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader("X-User-Role") Role role
    ) {
        // 공지는 boardType/idolId/groupId 고정
        req.setBoardType(BoardType.ADMIN_NOTICE);
        req.setIdolId(null);
        req.setGroupId(null);

        return postService.insert(req, userId, role);
    }

    // 공지 목록 (전체 공개)
    @GetMapping("/notices")
    public Page<PostListResponse> showNoticeList(Pageable pageable) {
        return postService.selectAll(BoardType.ADMIN_NOTICE, null, null, pageable);
    }

    // 공지 상세 (전체 공개)
    @GetMapping("/notices/{postId}")
    public PostResponse showNotice(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @RequestHeader(value = "X-User-Role", required = false) Role role
    ) {
        // 공지는 구독 체크 안 하므로, 헤더가 없어도 통과되게 기본값 처리
        Integer safeUserId = (userId == null) ? -1 : userId;
        Role safeRole = (role == null) ? Role.USER : role;

        return postService.selectOne(postId, safeUserId, safeRole);
    }

    // 공지 수정 (ADMIN만)
    @PutMapping("/notices/{postId}")
    public PostResponse updateNotice(
            @PathVariable Long postId,
            @RequestBody PostUpdateRequest req,
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader("X-User-Role") Role role
    ) {
        return postService.update(postId, req, userId, role);
    }

    // 공지 삭제 (ADMIN만)
    @DeleteMapping("/notices/{postId}")
    public void deleteNotice(
            @PathVariable Long postId,
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader("X-User-Role") Role role
    ) {
        postService.delete(postId, userId, role);
    }
}
