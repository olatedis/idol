package com.bit.idol.chatservice.filter;

public interface ChatFilter {
    // 메시지를 검사하여 문제가 있으면 예외를 던지거나 수정된 메시지를 반환
    String filter(String message);
}
