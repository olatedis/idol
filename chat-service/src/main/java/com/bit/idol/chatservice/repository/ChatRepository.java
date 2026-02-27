package com.bit.idol.chatservice.repository;

import com.bit.idol.chatservice.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatRepository extends MongoRepository<ChatMessage, String> {

    // 1. 커서 기반 페이징
    @Query(value = "{ 'idolId': ?0, '_id': { '$lt': ?1 } }", sort = "{ '_id': -1 }")
    List<ChatMessage> findByIdolIdAndIdLessThanOrderByIdDesc(Long idolId, String lastId, Pageable pageable);

    // 2. 최초 조회
    List<ChatMessage> findByIdolIdOrderByIdDesc(Long idolId, Pageable pageable);

    // --- 팬(USER) 전용 1:N 버블 필터링 조회 ---
    @Query(value = "{ 'idolId': ?0, '$or': [ { 'senderId': ?1 }, { 'senderRole': 'IDOL' } ], '_id': { '$lt': ?2 } }", sort = "{ '_id': -1 }")
    List<ChatMessage> findUserMessagesByIdolIdAndIdLessThanOrderByIdDesc(Long idolId, int userId, String lastId,
            Pageable pageable);

    @Query(value = "{ 'idolId': ?0, '$or': [ { 'senderId': ?1 }, { 'senderRole': 'IDOL' } ] }", sort = "{ '_id': -1 }")
    List<ChatMessage> findUserMessagesByIdolIdOrderByIdDesc(Long idolId, int userId, Pageable pageable);
    // ------------------------------------

    // 특정 유저가 보낸 메시지 조회
    List<ChatMessage> findByIdolIdAndSenderIdOrderByCreatedAtDesc(Long idolId, int senderId);

    // 3. 미디어 조회 (커서 기반)
    @Query(value = "{ 'idolId': ?0, 'type': { $in: ['IMAGE', 'VIDEO'] }, '_id': { '$lt': ?1 } }", sort = "{ '_id': -1 }")
    List<ChatMessage> findMediaByIdolIdAndIdLessThan(Long idolId, String lastId, Pageable pageable);

    // 4. 미디어 최초 조회
    @Query(value = "{ 'idolId': ?0, 'type': { $in: ['IMAGE', 'VIDEO'] } }", sort = "{ '_id': -1 }")
    List<ChatMessage> findMediaByIdolId(Long idolId, Pageable pageable);

    // --- 팬(USER) 전용 1:N 미디어 필터링 조회 ---
    @Query(value = "{ 'idolId': ?0, 'type': { $in: ['IMAGE', 'VIDEO'] }, '$or': [ { 'senderId': ?1 }, { 'senderRole': 'IDOL' } ], '_id': { '$lt': ?2 } }", sort = "{ '_id': -1 }")
    List<ChatMessage> findUserMediaByIdolIdAndIdLessThan(Long idolId, int userId, String lastId, Pageable pageable);

    @Query(value = "{ 'idolId': ?0, 'type': { $in: ['IMAGE', 'VIDEO'] }, '$or': [ { 'senderId': ?1 }, { 'senderRole': 'IDOL' } ] }", sort = "{ '_id': -1 }")
    List<ChatMessage> findUserMediaByIdolId(Long idolId, int userId, Pageable pageable);
    // ------------------------------------

    // --- Outbox Pattern (재전송 대상 조회) ---
    // status = 'PENDING' AND createdAt < ? (1분 전)
    List<ChatMessage> findByStatusAndCreatedAtBefore(String status, LocalDateTime time);

    // 전체 읽음 수 초기화 대응 쿼리
    long countByIdolIdAndSenderRole(Long idolId, String senderRole);
}
