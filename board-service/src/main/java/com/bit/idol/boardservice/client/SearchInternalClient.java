package com.bit.idol.boardservice.client;

import com.bit.idol.boardservice.dto.search.PageResponse;
import com.bit.idol.boardservice.dto.search.PostSearchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "search-service")
public interface SearchInternalClient {

    @GetMapping("/search/posts")
    PageResponse<PostSearchResponse> searchPosts(
            @RequestParam("boardType") String boardType,
            @RequestParam(value = "idolId", required = false) Long idolId,
            @RequestParam(value = "groupId", required = false) Long groupId,
            @RequestParam("keyword") String keyword,
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "sort", required = false) String sort
    );
}