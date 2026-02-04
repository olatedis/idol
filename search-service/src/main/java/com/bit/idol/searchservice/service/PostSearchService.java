package com.bit.idol.searchservice.service;

import com.bit.idol.searchservice.dto.PostSearchResponse;
import com.bit.idol.searchservice.document.PostDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.*;

@Service
@RequiredArgsConstructor
public class PostSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public Page<PostSearchResponse> searchPosts(String boardType, Long idolId, Long groupId, String keyword, Pageable pageable) {
        validateScope(boardType, idolId, groupId);

        // 스코프 필터: boardType + (idolId/groupId)
        var filter = boolQuery()
                .filter(termQuery("boardType", boardType));

        if (requiresIdolId(boardType)) {
            filter.filter(termQuery("idolId", idolId));
        }
        if (requiresGroupId(boardType)) {
            filter.filter(termQuery("groupId", groupId));
        }

        // 키워드 매칭: title + content
        // content는 응답에 포함하지 않지만, 검색 매칭에는 사용
        var keywordQuery = boolQuery()
                .must(filter)
                .must(multiMatchQuery(keyword, "title", "content"));

        NativeQuery query = NativeQuery.builder()
                .withQuery(keywordQuery)
                .withPageable(pageable)
                .build();

        SearchHits<PostDocument> hits = elasticsearchOperations.search(query, PostDocument.class);

        List<PostSearchResponse> content = hits.getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .map(this::toResponse)
                .collect(Collectors.toList());

        long total = hits.getTotalHits();
        return new PageImpl<>(content, pageable, total);
    }

    // 게시판 스코프 검증(1번 정책)
    private void validateScope(String boardType, Long idolId, Long groupId) {
        if (boardType == null || boardType.isBlank()) throw new RuntimeException("boardType은 필수입니다.");
        if (keywordInvalid(boardType)) throw new RuntimeException("유효하지 않은 boardType입니다.");

        // ADMIN_NOTICE는 idolId/groupId 없어야 함
        if ("ADMIN_NOTICE".equals(boardType)) {
            if (idolId != null || groupId != null) throw new RuntimeException("공지사항 검색에는 idolId/groupId가 없어야 합니다.");
            return;
        }

        // IDOL_*는 idolId 필수, groupId 금지
        if (requiresIdolId(boardType)) {
            if (idolId == null) throw new RuntimeException("아이돌 게시판 검색에는 idolId가 필요합니다.");
            if (groupId != null) throw new RuntimeException("아이돌 게시판 검색에는 groupId가 없어야 합니다.");
            return;
        }

        // GROUP_*는 groupId 필수, idolId 금지
        if (requiresGroupId(boardType)) {
            if (groupId == null) throw new RuntimeException("그룹 게시판 검색에는 groupId가 필요합니다.");
            if (idolId != null) throw new RuntimeException("그룹 게시판 검색에는 idolId가 없어야 합니다.");
        }
    }

    private boolean requiresIdolId(String boardType) {
        return "IDOL_OFFICIAL".equals(boardType) || "IDOL_FAN".equals(boardType);
    }

    private boolean requiresGroupId(String boardType) {
        return "GROUP_OFFICIAL".equals(boardType) || "GROUP_FAN".equals(boardType);
    }

    private boolean keywordInvalid(String boardType) {
        return !("IDOL_OFFICIAL".equals(boardType)
                || "IDOL_FAN".equals(boardType)
                || "GROUP_OFFICIAL".equals(boardType)
                || "GROUP_FAN".equals(boardType)
                || "ADMIN_NOTICE".equals(boardType));
    }

    private PostSearchResponse toResponse(PostDocument doc) {
        PostSearchResponse res = new PostSearchResponse();
        res.setPostId(doc.getPostId());
        res.setBoardType(doc.getBoardType());
        res.setIdolId(doc.getIdolId());
        res.setGroupId(doc.getGroupId());
        res.setTitle(doc.getTitle());
        res.setCreatedAt(doc.getCreatedAt());
        return res;
    }
}
