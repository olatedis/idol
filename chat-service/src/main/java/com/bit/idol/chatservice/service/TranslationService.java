package com.bit.idol.chatservice.service;

import com.bit.idol.chatservice.entity.ChatMessage;
import com.bit.idol.chatservice.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {

    private final ChatRepository chatRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${deepl.api-key:fake-key}") // API 키가 없으면 가짜 키 사용
    private String deeplApiKey;

    private static final String DEEPL_API_URL = "https://api-free.deepl.com/v2/translate";

    public String translateMessage(String messageId, String targetLang) {
        // 1. 메시지 조회
        ChatMessage message = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("메시지를 찾을 수 없습니다."));

        // 2. 이미 번역된 내용이 있는지 확인 (캐싱)
        Map<String, String> translations = message.getTranslations();
        if (translations == null) {
            translations = new HashMap<>();
        }

        String targetLangUpper = targetLang.toUpperCase();
        if (translations.containsKey(targetLangUpper)) {
            log.info("번역 캐시 적중: msgId={}, lang={}", messageId, targetLangUpper);
            return translations.get(targetLangUpper);
        }

        // 3. DeepL API 호출
        log.info("DeepL API 호출: msgId={}, lang={}", messageId, targetLangUpper);
        String translatedText = callDeepLApi(message.getContent(), targetLangUpper);

        // 4. 결과 저장 (캐싱)
        translations.put(targetLangUpper, translatedText);
        
        // 엔티티 업데이트 (Setter가 없으므로 Builder로 복사)
        ChatMessage updatedMessage = ChatMessage.builder()
                .id(message.getId())
                .idolId(message.getIdolId())
                .senderId(message.getSenderId())
                .senderNickname(message.getSenderNickname())
                .senderRole(message.getSenderRole())
                .content(message.getContent())
                .type(message.getType())
                .parentId(message.getParentId())
                .reactions(message.getReactions())
                .translations(translations) // 번역 추가
                .createdAt(message.getCreatedAt())
                .build();
        
        chatRepository.save(updatedMessage);

        return translatedText;
    }

    private String callDeepLApi(String text, String targetLang) {
        // API 키가 없거나 가짜 키면 더미 데이터 반환 (테스트용)
        if ("fake-key".equals(deeplApiKey) || deeplApiKey == null || deeplApiKey.isEmpty()) {
            return "[번역됨(" + targetLang + ")] " + text;
        }

        try {
            // DeepL API 요청
            // Response 구조: { "translations": [ { "detected_source_language": "KO", "text": "Hello" } ] }
            Map response = webClientBuilder.build()
                    .post()
                    .uri(DEEPL_API_URL)
                    .header("Authorization", "DeepL-Auth-Key " + deeplApiKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue("text=" + text + "&target_lang=" + targetLang)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("translations")) {
                List<Map<String, String>> translations = (List<Map<String, String>>) response.get("translations");
                if (!translations.isEmpty()) {
                    return translations.get(0).get("text");
                }
            }
        } catch (Exception e) {
            log.error("DeepL API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("번역 서비스가 일시적으로 불가능합니다.");
        }

        return text; // 실패 시 원본 반환
    }
}
