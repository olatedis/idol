package com.bit.idol.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private String id; // MongoDB ID (페이징 커서용)
    private Long idolId; // 방 번호
    private int senderId;
    private String senderNickname;
    private String senderRole; // USER or IDOL
    private String content;
    private String type; // TALK, IMAGE, VIDEO, VOICE
}
