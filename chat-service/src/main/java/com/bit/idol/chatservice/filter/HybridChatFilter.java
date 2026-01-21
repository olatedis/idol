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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

@Slf4j
@Component
@Primary
public class HybridChatFilter implements ChatFilter {

    private AhoCorasickDoubleArrayTrie<String> acTrie;

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
            // 라이브러리 요구사항: 키가 정렬된 Map이어야 함
            TreeMap<String, String> map = new TreeMap<>();
            for (String word : badWords) {
                map.put(word, word);
            }

            acTrie = new AhoCorasickDoubleArrayTrie<>();
            acTrie.build(map);
            
            log.info("욕설 필터 로딩 완료. (단어 수: {})", badWords.size());

        } catch (Exception e) {
            log.error("욕설 필터 로딩 실패", e);
            // 실패해도 서버는 켜져야 하므로 빈 Trie로 초기화 (또는 예외 던져서 서버 종료)
            acTrie = new AhoCorasickDoubleArrayTrie<>();
        }
    }

    @Override
    public String filter(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }

        // 1. 정규화 (Normalization)
        // 특수문자, 공백 제거 및 소문자 변환
        // 예: "시. .발" -> "시발", "F.u.c.k" -> "fuck"
        String normalized = normalize(message);

        // 2. 욕설 탐지 (Aho-Corasick)
        List<AhoCorasickDoubleArrayTrie.Hit<String>> hits = acTrie.parseText(normalized);

        if (!hits.isEmpty()) {
            // 욕설 발견!
            String detectedWord = hits.get(0).value;
            log.warn("욕설 감지됨: origin='{}', detected='{}'", message, detectedWord);
            throw new RuntimeException("부적절한 단어가 포함되어 있습니다.");
        }

        // 3. AI 검사 (추후 구현)
        // if (aiClient.inspect(message).isHarmful()) { ... }

        return message;
    }

    private String normalize(String text) {
        // 1. 소문자 변환
        String lower = text.toLowerCase();
        
        // 2. 특수문자 및 공백 제거 (한글, 영문, 숫자만 남김)
        String clean = lower.replaceAll("[^가-힣a-z0-9]", "");
        
        // 3. 유사 문자 치환 (Leetspeak 방어 - 예시)
        // clean = clean.replace("1", "i").replace("0", "o").replace("@", "a");
        
        return clean;
    }
}
