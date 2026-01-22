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

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ChatSearchRepository chatSearchRepository;
    private final SubscriptionFeignClient subscriptionFeignClient;
    private final StringRedisTemplate redisTemplate;

    private static final String SUBSCRIPTION_CACHE_KEY = "search:sub:user:%d:idol:%d";

    public Page<ChatDocument> searchChat(int userId, Long idolId, String keyword, Pageable pageable) {
        // 1. 구독 권한 체크 (Redis -> Feign -> Circuit Breaker)
        if (!hasSubscription(userId, idolId)) {
            throw new RuntimeException("구독하지 않은 아이돌의 채팅은 검색할 수 없습니다.");
        }

        // 2. Elasticsearch 검색
        return chatSearchRepository.findByIdolIdAndContentContaining(idolId, keyword, pageable);
    }

    // 구독 체크 로직 (Redis 캐싱 + 서킷 브레이커)
    @CircuitBreaker(name = "subscription-check", fallbackMethod = "fallbackSubscriptionCheck")
    public boolean hasSubscription(int userId, Long idolId) {
        String key = String.format(SUBSCRIPTION_CACHE_KEY, userId, idolId);

        // 1. Redis 캐시 확인
        String cachedValue = redisTemplate.opsForValue().get(key);
        if (cachedValue != null) {
            return Boolean.parseBoolean(cachedValue);
        }

        // 2. FeignClient 호출 (실제 서비스 확인)
        boolean isSubscribed = subscriptionFeignClient.checkSubscription(userId, idolId);

        // 3. 결과 Redis 저장 (24시간 캐싱 - 수정됨)
        redisTemplate.opsForValue().set(key, String.valueOf(isSubscribed), Duration.ofHours(24));

        return isSubscribed;
    }

    // 서킷 브레이커 Fallback (장애 발생 시 실행)
    public boolean fallbackSubscriptionCheck(int userId, Long idolId, Throwable t) {
        log.error("구독 서비스 장애 발생: userId={}, idolId={}, error={}", userId, idolId, t.getMessage());
        
        // 장애 시 정책 결정:
        // 여기서는 "보안"이 중요하므로 장애 시에는 검색을 막는 것이 안전함.
        return false; 
    }
}
