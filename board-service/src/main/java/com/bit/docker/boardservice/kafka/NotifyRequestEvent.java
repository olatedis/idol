package com.bit.docker.boardservice.kafka;

import lombok.Data;

import java.util.Map;


@Data
public class NotifyRequestEvent {
    private String eventId;
    private String type;
    private String targetType;
    private String targetId;
    private Map<String, String> args;
    private String redirectUrl;
    private String occurredAt;
}
