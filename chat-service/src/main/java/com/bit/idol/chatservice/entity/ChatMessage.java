package com.bit.idol.chatservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "chat_messages")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    @Id
    private String id;

    private Long idolId; // 채팅방 ID (아이돌 ID)
    private int senderId; // 보낸 사람 ID
    private String senderNickname;
    private String senderRole; // USER or IDOL
    
    private String content; // 메시지 내용
    private String type; // TALK, IMAGE, SYSTEM, DELETED 등

    // 답장 기능 (원본 메시지 ID)
    private String parentId;

    // 반응 기능 (이모지별 카운트)
    // 예: { "HEART": 10, "LIKE": 5 }
    @Builder.Default
    private Map<String, Integer> reactions = new HashMap<>();

    // 번역 기능 (언어별 번역본 캐싱)
    // 예: { "EN": "Hello", "JA": "こんにちは" }
    @Builder.Default
    private Map<String, String> translations = new HashMap<>();

    private LocalDateTime createdAt;
}
