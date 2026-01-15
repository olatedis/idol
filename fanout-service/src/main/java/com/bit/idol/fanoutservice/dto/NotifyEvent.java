package com.bit.idol.fanoutservice.dto;

import lombok.Data;

@Data
public class NotifyEvent {
    private String eventId;      // UUID
    private String eventType;    // 예: VOTE_OPENED
    private String occurredAt;   // ISO-8601 문자열로 받는다고 가정 (서비스들끼리 통일)
    private String producer;     // 예: vote-service
    private NotifyData data;     // 실제 알림 데이터
}
