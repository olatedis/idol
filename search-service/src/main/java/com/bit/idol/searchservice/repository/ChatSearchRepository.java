package com.bit.idol.searchservice.repository;

import com.bit.idol.searchservice.document.ChatDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ChatSearchRepository extends ElasticsearchRepository<ChatDocument, String> {
    
    // 아이돌 ID로 필터링하고 내용 검색 (최신순 정렬은 Pageable로 처리)
    Page<ChatDocument> findByIdolIdAndContentContaining(Long idolId, String content, Pageable pageable);
}
