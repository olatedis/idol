package com.bit.idol.chatservice.repository;

import com.bit.idol.chatservice.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatRepository extends MongoRepository<ChatMessage, String> {
    
    // 1. 커서 기반 페이징
    @Query("{ 'idolId': ?0, '_id': { '$lt': ?1 } }")
    List<ChatMessage> findByIdolIdAndIdLessThanOrderByIdDesc(Long idolId, String lastId, Pageable pageable);

    // 2. 최초 조회
    List<ChatMessage> findByIdolIdOrderByIdDesc(Long idolId, Pageable pageable);
    
    // 특정 유저가 보낸 메시지 조회
    List<ChatMessage> findByIdolIdAndSenderIdOrderByCreatedAtDesc(Long idolId, int senderId);

    // 3. 미디어 조회 (커서 기반)
    @Query("{ 'idolId': ?0, 'type': { $in: ['IMAGE', 'VIDEO'] }, '_id': { '$lt': ?1 } }")
    List<ChatMessage> findMediaByIdolIdAndIdLessThan(Long idolId, String lastId, Pageable pageable);

    // 4. 미디어 최초 조회
    @Query("{ 'idolId': ?0, 'type': { $in: ['IMAGE', 'VIDEO'] } }")
    List<ChatMessage> findMediaByIdolId(Long idolId, Pageable pageable);

    // --- Outbox Pattern (재전송 대상 조회) ---
    // status = 'PENDING' AND createdAt < ? (1분 전)
    List<ChatMessage> findByStatusAndCreatedAtBefore(String status, LocalDateTime time);
}
