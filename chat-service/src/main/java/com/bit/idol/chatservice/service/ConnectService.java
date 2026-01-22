package com.bit.idol.chatservice.service;

import com.bit.idol.chatservice.client.SubscriptionFeignClient;
import com.bit.idol.chatservice.client.UserFeignClient;
import com.bit.idol.chatservice.dto.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectService {

    private final UserFeignClient userFeignClient;
    private final SubscriptionFeignClient subscriptionFeignClient;

    // 1. 토큰 검증 (User Service 호출)
    @CircuitBreaker(name = "user-check", fallbackMethod = "fallbackVerifyUser")
    public UserDto verifyUser(String token) {
        return userFeignClient.getUserInfo(token);
    }

    // 2. 구독 확인 (Subscription Service 호출)
    @CircuitBreaker(name = "subscription-check", fallbackMethod = "fallbackVerifySubscription")
    public boolean verifySubscription(int userId, Long idolId) {
        return subscriptionFeignClient.checkSubscription(userId, idolId);
    }

    // Fallback: 유저 검증 실패 시 (User Service 장애)
    public UserDto fallbackVerifyUser(String token, Throwable t) {
        log.error("User Service 장애 발생: {}", t.getMessage());
        // 장애 시 접속 차단 (보안 우선)
        throw new RuntimeException("로그인 서버가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요.");
    }

    // Fallback: 구독 확인 실패 시 (Subscription Service 장애)
    public boolean fallbackVerifySubscription(int userId, Long idolId, Throwable t) {
        log.error("Subscription Service 장애 발생: userId={}, idolId={}, error={}", userId, idolId, t.getMessage());
        // 장애 시 접속 차단 (보안 우선)
        throw new RuntimeException("구독 정보를 확인할 수 없어 입장이 제한됩니다.");
    }
}
