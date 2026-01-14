package com.bit.idol.notifyservice.dto;

import java.util.List;

public class NotificationListResponse {
    public List<NotificationItemResponse> items;
    public String nextCursor; // 다음페이지 불러올때 기준되는 시간값(ISO)
    public boolean hasNext;
}
