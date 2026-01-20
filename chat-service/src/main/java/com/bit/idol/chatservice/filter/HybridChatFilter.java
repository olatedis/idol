package com.bit.idol.chatservice.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
@Primary
public class HybridChatFilter implements ChatFilter {

    // 1차: 정규표현식 (욕설 리스트 - 예시)
    private static final Pattern BAD_WORDS = Pattern.compile(".*(시발|개새끼|죽어|바보).*");

    @Override
    public String filter(String message) {
        // 1. 로컬 검사 (빠름)
        if (BAD_WORDS.matcher(message).matches()) {
            log.warn("욕설 감지됨: {}", message);
            throw new RuntimeException("부적절한 단어가 포함되어 있습니다.");
        }

        // 2. AI 검사 (추후 구현)
        // if (aiClient.inspect(message).isHarmful()) { ... }

        return message;
    }
}
