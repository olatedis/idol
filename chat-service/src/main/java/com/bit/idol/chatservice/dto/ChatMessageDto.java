package com.bit.idol.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
    private String thumbnailUrl; // 이미지 전용 썸네일 URL
    private String senderProfileImage; // 발신자 프로필 이미지 추가
    private String parentId; // 답장할 메시지 ID
    private Map<String, Integer> reactions; // 반응 (좋아요 등)
    private String deleteReason; // 삭제 사유 (AI_FILTERED 등)
    private LocalDateTime createdAt; // 발송 시간 추가

    // 조회 시점에 계산되는 필드
    private boolean isMe; // 내가 보낸 메시지인지 여부
}
