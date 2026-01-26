package com.bit.docker.boardservice.controller;

import com.bit.docker.boardservice.dto.PostListResponse;
import com.bit.docker.boardservice.dto.PostUpdateRequest;
import com.bit.docker.boardservice.dto.PostWriteRequest;
import com.bit.docker.boardservice.entity.BoardType;
import com.bit.docker.boardservice.service.PostService;
import com.bit.docker.boardservice.service.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/board/posts")
@Slf4j
public class PostController {

    private final PostService postService;

    // 목록 조회
    @GetMapping
    public ResponseEntity<?> showAll(
            @RequestParam("boardType") BoardType boardType,
            @RequestParam(value = "idolId", required = false) Long idolId,
            @RequestParam(value = "groupId", required = false) Long groupId,
            Pageable pageable
    ) {
        Page<PostListResponse> page = postService.selectAll(boardType, idolId, groupId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<?> showOne(
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader("X-User-Role") String roleRaw,
            @PathVariable("postId") Long postId
    ) {
        Role role = parseRole(roleRaw);
        return ResponseEntity.ok(postService.selectOne(postId, userId, role));
    }

    // 작성
    @PostMapping
    public ResponseEntity<?> write(
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader("X-User-Role") String roleRaw,
            @RequestBody PostWriteRequest req
    ) {
        Role role = parseRole(roleRaw);
        return ResponseEntity.ok(postService.insert(req, userId, role));
    }

    // 수정
    @PutMapping("/{postId}")
    public ResponseEntity<?> update(
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader("X-User-Role") String roleRaw,
            @PathVariable("postId") Long postId,
            @RequestBody PostUpdateRequest req
    ) {
        Role role = parseRole(roleRaw);
        return ResponseEntity.ok(postService.update(postId, req, userId, role));
    }

    // 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> delete(
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader("X-User-Role") String roleRaw,
            @PathVariable("postId") Long postId
    ) {
        Role role = parseRole(roleRaw);
        postService.delete(postId, userId, role);
        return ResponseEntity.ok().build();
    }

    private Role parseRole(String raw) {
        try {
            Role role = Role.from(raw);
            if (role == null) {
                throw new ResponseStatusException(UNAUTHORIZED, "role is required");
            }
            return role;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            // raw 값이 enum 변환 실패 등인 경우
            throw new ResponseStatusException(UNAUTHORIZED, "invalid role", e);
        }
    }
}
