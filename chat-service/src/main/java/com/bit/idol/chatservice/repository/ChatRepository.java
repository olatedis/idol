package com.bit.idol.chatservice.repository;

import com.bit.idol.chatservice.entity.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatRepository extends MongoRepository<ChatMessage, String> {
    // 특정 아이돌 방의 메시지 조회 (최신순 or 과거순)
    List<ChatMessage> findByIdolIdOrderByCreatedAtDesc(Long idolId);
    
    // 특정 유저가 보낸 메시지 조회 (1:1 내역 보기용)
    List<ChatMessage> findByIdolIdAndSenderIdOrderByCreatedAtDesc(Long idolId, int senderId);
}
