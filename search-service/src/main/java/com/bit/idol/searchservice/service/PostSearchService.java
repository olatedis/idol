package com.bit.idol.searchservice.service;

import com.bit.idol.searchservice.document.PostDocument;
import com.bit.idol.searchservice.dto.PostSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.*;

@Service
@RequiredArgsConstructor
public class PostSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public Page<PostSearchResponse> searchPosts(String boardType, Long idolId, Long groupId, String keyword, Pageable pageable) {
        validateScope(boardType, idolId, groupId);

        if (keyword == null || keyword.trim().isEmpty()) {
            throw new RuntimeException("keyword는 필수입니다.");
        }
        String k = keyword.trim();

        // 1) 스코프 쿼리 (boardType + idolId/groupId)
        var bool = bool();

        bool.filter(term(t -> t.field("boardType").value(boardType)));

        if (requiresIdolId(boardType)) {
            bool.filter(term(t -> t.field("idolId").value(idolId)));
        }
        if (requiresGroupId(boardType)) {
            bool.filter(term(t -> t.field("groupId").value(groupId)));
        }

        // 2) 키워드 쿼리 (title OR content)
        // - wildcard는 느릴 수 있어서 match + operator OR로 처리
        // - 정확히 "포함" 느낌을 원하면 query_string 사용도 가능
        bool.must(bool(b -> b
                .should(match(m -> m.field("title").query(k)))
                .should(match(m -> m.field("content").query(k)))
                .minimumShouldMatch("1")
        ));

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(bool.build()))
                .withPageable(pageable)
                .build();

        SearchHits<PostDocument> hits = elasticsearchOperations.search(query, PostDocument.class);

        List<PostSearchResponse> content = hits.getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }

    // 이하 validateScope / requiresIdolId / requiresGroupId / isValidBoardType / toResponse는 기존 그대로 유지
    private void validateScope(String boardType, Long idolId, Long groupId) {
        if (boardType == null || boardType.isBlank()) throw new RuntimeException("boardType은 필수입니다.");

        if (!isValidBoardType(boardType)) {
            throw new RuntimeException("유효하지 않은 boardType입니다.");
        }

        if ("ADMIN_NOTICE".equals(boardType)) {
            if (idolId != null || groupId != null) throw new RuntimeException("공지사항 검색에는 idolId/groupId가 없어야 합니다.");
            return;
        }

        if (requiresIdolId(boardType)) {
            if (idolId == null) throw new RuntimeException("아이돌 게시판 검색에는 idolId가 필요합니다.");
            if (groupId != null) throw new RuntimeException("아이돌 게시판 검색에는 groupId가 없어야 합니다.");
            return;
        }

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

    private boolean isValidBoardType(String boardType) {
        return "IDOL_OFFICIAL".equals(boardType)
                || "IDOL_FAN".equals(boardType)
                || "GROUP_OFFICIAL".equals(boardType)
                || "GROUP_FAN".equals(boardType)
                || "ADMIN_NOTICE".equals(boardType);
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