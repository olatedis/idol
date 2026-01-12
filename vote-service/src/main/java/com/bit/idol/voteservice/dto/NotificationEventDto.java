package com.bit.idol.voteservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventDto {
    
    private String eventId;
    private String eventType;
    private String occurredAt;
    private String producer;
    private int version;
    private NotificationData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationData {
        private int receiverId;
        private String category;
        private String title;
        private String body;
        private String deeplink;
        private String refType;
        private int refId;
        private Map<String, Object> attributes;
    }
}
