package com.bit.idol.searchservice.controller;

import com.bit.idol.searchservice.dto.PostSearchResponse;
import com.bit.idol.searchservice.service.PostSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
public class PostSearchController {

    private final PostSearchService postSearchService;

    // 게시글 검색(게시판 스코프 내에서만 검색)
    // 예) /search/posts?boardType=IDOL_OFFICIAL&idolId=1&keyword=안녕
    // 예) /search/posts?boardType=ADMIN_NOTICE&keyword=점검
    @GetMapping("/posts")
    public ResponseEntity<Page<PostSearchResponse>> searchPosts(
            @RequestParam("boardType") String boardType,
            @RequestParam(value = "idolId", required = false) Long idolId,
            @RequestParam(value = "groupId", required = false) Long groupId,
            @RequestParam("keyword") String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("게시글 검색 요청: boardType={}, idolId={}, groupId={}, keyword={}", boardType, idolId, groupId, keyword);
        Page<PostSearchResponse> result = postSearchService.searchPosts(boardType, idolId, groupId, keyword, pageable);
        return ResponseEntity.ok(result);
    }
}
