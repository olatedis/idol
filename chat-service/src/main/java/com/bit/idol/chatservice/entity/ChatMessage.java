package com.bit.idol.chatservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "chat_messages")
@CompoundIndexes({
        @CompoundIndex(name = "idx_idol_id_desc", def = "{'idolId': 1, '_id': -1}"),
        @CompoundIndex(name = "idx_status_created_at", def = "{'status': 1, 'createdAt': 1}") // 재전송 조회용 인덱스
})
@Getter
@Setter // Setter 추가 (상태 변경용)
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
    private String thumbnailUrl; // 이미지 썸네일 URL
    private String senderProfileImage; // 발신자 프로필 이미지 추가

    // 답장 기능 (원본 메시지 ID)
    private String parentId;

    // 반응 기능 (이모지별 카운트)
    @Builder.Default
    private Map<String, Integer> reactions = new HashMap<>();

    // 번역 기능 (언어별 번역본 캐싱)
    @Builder.Default
    private Map<String, String> translations = new HashMap<>();

    private LocalDateTime createdAt;

    // --- Outbox Pattern ---
    @Builder.Default
    private String status = "PENDING"; // PENDING, SENT
}
