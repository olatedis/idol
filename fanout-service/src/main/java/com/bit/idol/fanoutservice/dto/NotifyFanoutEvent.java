package com.bit.idol.fanoutservice.dto;

import lombok.Data;

import java.util.Map;

// fanout용
@Data
public class NotifyFanoutEvent {
    private String eventId;
    private String type;
    private TargetType targetType;
    private String targetId;
    private Map<String, String> args;
    private String redirectUrl;
    private String occurredAt;
}
