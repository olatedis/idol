package com.bit.idol.notifyservice.dto;

import java.util.Map;

public class NotificationItemResponse {
    public int notificationId;

    public String category;
    public String eventType;
    public String title;
    public String body;
    public String deeplink;

    public Map<String, Object> attributes;

    public String createdAt;
    public String readAt;
}
