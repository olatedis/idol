package com.bit.idol.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomListDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long idolId;
    private String idolName;
    private String thumbnailUrl; // 아이돌 프로필 이미지
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private int unreadCount; // 안 읽은 메시지 수
    private boolean isSubscribed; // 구독 여부
}
