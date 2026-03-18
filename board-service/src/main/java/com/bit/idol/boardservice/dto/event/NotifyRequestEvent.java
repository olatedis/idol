package com.bit.idol.boardservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyRequestEvent {
    private String eventId;      // 중복 방지용 UUID
    private String type;         // 알림 종류 (COMMENT_ADDED, POST_LIKED 등)
    private TargetType targetType;
    private String targetId;     // userId, idolId, groupId 등
    private Map<String, String> args;
    private String redirectUrl;
    private String occurredAt;   // ISO_LOCAL_DATE_TIME
}
