package com.bit.idol.searchservice.service;

import com.bit.idol.searchservice.client.SubscriptionFeignClient;
import com.bit.idol.searchservice.document.ChatDocument;
import com.bit.idol.searchservice.repository.ChatSearchRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ChatSearchRepository chatSearchRepository;
    private final SubscriptionFeignClient subscriptionFeignClient;
    private final StringRedisTemplate redisTemplate;

    private static final String SUBSCRIPTION_CACHE_KEY = "search:sub:user:%d:idol:%d";

    public Page<ChatDocument> searchChat(int userId, Long idolId, String keyword, Pageable pageable) {
        if (!hasSubscription(userId, idolId)) {
            throw new RuntimeException("구독하지 않은 아이돌의 채팅은 검색할 수 없습니다.");
        }
        return chatSearchRepository.findByIdolIdAndContentContaining(idolId, keyword, pageable);
    }

    // 자동 완성 기능은 일단 제거 (추후 구현)
    public List<String> autoComplete(Long idolId, String prefix) {
        return List.of(); 
    }

    @CircuitBreaker(name = "subscription-check", fallbackMethod = "fallbackSubscriptionCheck")
    public boolean hasSubscription(int userId, Long idolId) {
        String key = String.format(SUBSCRIPTION_CACHE_KEY, userId, idolId);

        String cachedValue = redisTemplate.opsForValue().get(key);
        if (cachedValue != null) {
            return Boolean.parseBoolean(cachedValue);
        }

        boolean isSubscribed = subscriptionFeignClient.checkSubscription(userId, idolId);

        redisTemplate.opsForValue().set(key, String.valueOf(isSubscribed), Duration.ofHours(24));

        return isSubscribed;
    }

    public boolean fallbackSubscriptionCheck(int userId, Long idolId, Throwable t) {
        log.error("구독 서비스 장애 발생: userId={}, idolId={}, error={}", userId, idolId, t.getMessage());
        return false; 
    }
}
