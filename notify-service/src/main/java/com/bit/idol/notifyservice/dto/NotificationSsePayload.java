package com.bit.idol.notifyservice.dto;

import lombok.Data;

import java.util.Map;

@Data
public class NotificationSsePayload {
    private int notificationId;
    private String eventId;
    private String type;

    private String targetType;
    private String targetId;

    private Map<String, String> args;

    private String redirectUrl;
    private String occurredAt;
}