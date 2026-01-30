package com.bit.idol.notifyservice.dto;

import lombok.Data;

@Data
public class IdolMessageStackSsePayload {
    private long idolId;
    private int unreadCount;
    private String lastOccurredAt;
}
