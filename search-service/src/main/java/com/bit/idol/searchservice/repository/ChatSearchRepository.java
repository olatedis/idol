package com.bit.idol.searchservice.repository;

import com.bit.idol.searchservice.document.ChatDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.annotations.Query;

public interface ChatSearchRepository extends ElasticsearchRepository<ChatDocument, String> {

    // 명시적인 Elasticsearch bool + match 쿼리 (띄어쓰기가 포함된 문장도 분석하여 검색)
    @Query("{\"bool\": {\"must\": [{\"term\": {\"idolId\": \"?0\"}}, {\"match\": {\"content\": \"?1\"}}]}}")
    Page<ChatDocument> searchByKeyword(Long idolId, String keyword, Pageable pageable);
}
