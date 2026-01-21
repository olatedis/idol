package com.bit.idol.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private String id; // MongoDB ID
    private Long idolId; // 방 번호
    private int senderId;
    private String senderNickname;
    private String senderRole; // USER or IDOL
    private String content;
    private String type; // TALK, IMAGE, VIDEO, VOICE, REACTION, DELETE
    
    // 답장 기능
    private String parentId;
    
    // 반응 기능 (전송 시에는 "HEART" 같은 타입만 오고, 조회 시에는 카운트 맵이 나감)
    private String reactionType; // 클라이언트가 보낼 때 (예: "HEART")
    private Map<String, Integer> reactions; // 클라이언트가 받을 때 (예: {"HEART": 10})
}
