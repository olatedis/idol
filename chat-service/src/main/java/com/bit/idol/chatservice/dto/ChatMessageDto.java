package com.bit.idol.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private String id;
    private Long idolId;
    private int senderId;
    private String senderNickname;
    private String senderRole;
    private String content;
    private String type; // TEXT, IMAGE, VIDEO, VOICE, TYPING, PIN, UNPIN, DELETE, REACTION
    private String parentId; // 답장할 메시지 ID
    private Map<String, Integer> reactions; // 반응 (좋아요 등)
    
    // 조회 시점에 계산되는 필드
    private boolean isMe; // 내가 보낸 메시지인지 여부
}
