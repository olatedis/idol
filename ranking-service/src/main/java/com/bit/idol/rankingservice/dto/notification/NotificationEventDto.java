package com.bit.idol.rankingservice.dto.notification;

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
public class NotificationEventDto {
    private String eventId;          // UUID
    private String type;             // 알림 종류
    private TargetType targetType;   // 대상 타입
    private String targetId;         // 대상 ID
    private Map<String, String> args; // 치환 변수
    private String redirectUrl;      // 클릭 시 이동할 주소
    private LocalDateTime occurredAt; // 발생 시간
}
