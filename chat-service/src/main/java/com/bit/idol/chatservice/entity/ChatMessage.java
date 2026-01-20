package com.bit.idol.chatservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

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
    private String type; // TALK, IMAGE, SYSTEM 등

    private LocalDateTime createdAt;
}
