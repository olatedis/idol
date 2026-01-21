package com.bit.idol.chatservice.filter;

import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Slf4j
@Component
@Primary
public class HybridChatFilter implements ChatFilter {

    private AhoCorasickDoubleArrayTrie<String> acTrie;
    
    // 유사 문자 치환 테이블
    private static final Map<Character, Character> LEET_MAP = new HashMap<>();

    static {
        // 숫자 -> 알파벳
        LEET_MAP.put('0', 'o');
        LEET_MAP.put('1', 'i');
        LEET_MAP.put('2', 'z');
        LEET_MAP.put('3', 'e');
        LEET_MAP.put('4', 'a');
        LEET_MAP.put('5', 's');
        LEET_MAP.put('6', 'g');
        LEET_MAP.put('7', 't');
        LEET_MAP.put('8', 'b');
        LEET_MAP.put('9', 'g'); // or q

        // 특수문자 -> 알파벳
        LEET_MAP.put('@', 'a');
        LEET_MAP.put('$', 's');
        LEET_MAP.put('!', 'i');
        LEET_MAP.put('|', 'i');
    }

    @PostConstruct
    public void init() {
        try {
            log.info("욕설 필터 데이터 로딩 시작...");
            Set<String> badWords = new HashSet<>();
            
            // 1. 파일에서 욕설 읽기
            ClassPathResource resource = new ClassPathResource("badwords.txt");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        badWords.add(line.trim().toLowerCase()); // 소문자로 저장
                    }
                }
            }

            // 2. Aho-Corasick Trie 구축
            TreeMap<String, String> map = new TreeMap<>();
            for (String word : badWords) {
                map.put(word, word);
            }

            acTrie = new AhoCorasickDoubleArrayTrie<>();
            acTrie.build(map);
            
            log.info("욕설 필터 로딩 완료. (단어 수: {})", badWords.size());

        } catch (Exception e) {
            log.error("욕설 필터 로딩 실패", e);
            acTrie = new AhoCorasickDoubleArrayTrie<>();
        }
    }

    @Override
    public String filter(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }

        // 1. 정규화 (Normalization)
        // 특수문자 제거, 소문자 변환, 유사 문자 치환
        String normalized = normalize(message);

        // 2. 욕설 탐지 (Aho-Corasick)
        List<AhoCorasickDoubleArrayTrie.Hit<String>> hits = acTrie.parseText(normalized);

        if (!hits.isEmpty()) {
            String detectedWord = hits.get(0).value;
            log.warn("욕설 감지됨: origin='{}', normalized='{}', detected='{}'", message, normalized, detectedWord);
            throw new RuntimeException("부적절한 단어가 포함되어 있습니다.");
        }

        return message;
    }

    private String normalize(String text) {
        StringBuilder sb = new StringBuilder();
        
        // 소문자로 변환하여 한 글자씩 순회
        for (char c : text.toLowerCase().toCharArray()) {
            // 1. 한글, 영문인 경우 그대로 사용
            if ((c >= 'a' && c <= 'z') || (c >= '가' && c <= '힣')) {
                sb.append(c);
            }
            // 2. 유사 문자(숫자, 특수문자)인 경우 치환
            else if (LEET_MAP.containsKey(c)) {
                sb.append(LEET_MAP.get(c));
            }
            // 3. 그 외(공백, 의미 없는 특수문자)는 무시 (삭제)
        }
        
        return sb.toString();
    }
}
