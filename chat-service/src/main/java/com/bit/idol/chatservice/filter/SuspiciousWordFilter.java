package com.bit.idol.chatservice.filter;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SuspiciousWordFilter {

    // 의심 단어 목록 (욕설은 아니지만 분쟁 유발 가능성)
    // 실제로는 DB나 파일에서 관리하는 것이 좋음
    private static final Set<String> SUSPICIOUS_WORDS = Set.of(
            "싸움", "정치", "종교", "환불", "사기", "폭력", "자살", "죽어", "미친", "바보",
            "AI차단" // 테스트용
    );

    public boolean isSuspicious(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        
        // 간단한 포함 여부 검사 (성능을 위해 Aho-Corasick을 써도 됨)
        for (String word : SUSPICIOUS_WORDS) {
            if (content.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
