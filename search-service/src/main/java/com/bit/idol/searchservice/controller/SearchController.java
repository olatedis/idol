package com.bit.idol.searchservice.controller;

import com.bit.idol.searchservice.document.ChatDocument;
import com.bit.idol.searchservice.service.SearchService;
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
public class SearchController {

    private final SearchService searchService;

    // 채팅 검색 API
    // GET /search/chat?idolId=1&keyword=안녕
    @GetMapping("/chat")
    public ResponseEntity<Page<ChatDocument>> searchChat(
            @RequestHeader("X-User-Id") int userId, // Gateway에서 주입
            @RequestParam("idolId") Long idolId,
            @RequestParam("keyword") String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("채팅 검색 요청: userId={}, idolId={}, keyword={}", userId, idolId, keyword);
        Page<ChatDocument> result = searchService.searchChat(userId, idolId, keyword, pageable);
        return ResponseEntity.ok(result);
    }
}
