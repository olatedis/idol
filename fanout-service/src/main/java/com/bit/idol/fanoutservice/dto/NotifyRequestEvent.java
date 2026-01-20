package com.bit.idol.fanoutservice.dto;

import lombok.Data;

import java.util.Map;

// notify-request-topic으로 들어오는 "요청" 이벤트
@Data
public class NotifyRequestEvent {
    private String eventId;              // uuid
    private String type;
    private TargetType targetType;
    private String targetId;             // USER면 userId, ALL이면 null 가능
    private Map<String, String> args;
    private String redirectUrl;
    private String occurredAt;
}
