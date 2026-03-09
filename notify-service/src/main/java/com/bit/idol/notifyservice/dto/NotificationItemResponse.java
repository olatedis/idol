package com.bit.idol.notifyservice.dto;

import java.util.Map;

public class NotificationItemResponse {
    public int notificationId;

    public String eventId;
    public String type;

    public String targetType;
    public String targetId;

    public Map<String, String> args;

    public String redirectUrl;
    public String occurredAt;

    public String readAt;
    public boolean isRead;
}
