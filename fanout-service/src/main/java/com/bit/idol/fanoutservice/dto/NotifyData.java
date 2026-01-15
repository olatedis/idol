package com.bit.idol.fanoutservice.dto;

import lombok.Data;

import java.util.Map;

@Data
public class NotifyData {
    private Integer receiverId;
    private String category;            // CHAT, VOTE, TICKET, NOTICE, SYSTEM
    private String title;
    private String body;
    private String deeplink;
    private Map<String, Object> attributes; // 확장 메타: target 등
}
