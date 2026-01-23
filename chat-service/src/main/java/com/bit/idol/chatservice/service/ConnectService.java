package com.bit.idol.chatservice.service;

import com.bit.idol.chatservice.client.SubscriptionFeignClient;
import com.bit.idol.chatservice.dto.UserDto;
import com.bit.idol.grpc.AuthGrpcServiceGrpc;
import com.bit.idol.grpc.AuthResponse;
import com.bit.idol.grpc.TokenRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectService {

    @GrpcClient("auth-service")
    private AuthGrpcServiceGrpc.AuthGrpcServiceBlockingStub authStub;

    private final SubscriptionFeignClient subscriptionFeignClient;
    private final RedisTemplate<String, Object> redisTemplate; // Redis 추가

    private static final String SESSION_KEY_PREFIX = "chat:session:";

    // 1. 토큰 검증 (Auth Service gRPC 호출)
    @CircuitBreaker(name = "user-check", fallbackMethod = "fallbackVerifyUser")
    public UserDto verifyUser(String token) {
        TokenRequest request = TokenRequest.newBuilder()
                .setToken(token)
                .build();

        try {
            AuthResponse response = authStub.verifyToken(request);

            if (!response.getIsValid()) {
                throw new RuntimeException("유효하지 않은 토큰입니다.");
            }

            return UserDto.builder()
                    .userId(Integer.parseInt(response.getUserId()))
                    .nickname(response.getNickname())
                    .role(response.getRole())
                    .build();

        } catch (StatusRuntimeException e) {
            log.error("gRPC 호출 실패: {}", e.getStatus());
            throw new RuntimeException("인증 서버 통신 오류");
        }
    }

    // 2. 구독 확인 (Subscription Service 호출)
    @CircuitBreaker(name = "subscription-check", fallbackMethod = "fallbackVerifySubscription")
    public boolean verifySubscription(int userId, Long idolId) {
        return subscriptionFeignClient.checkSubscription(userId, idolId);
    }

    // --- Redis 세션 관리 (추가됨) ---

    public void saveUserSession(String sessionId, UserDto userDto) {
        // 세션 유지 시간: 24시간 (WebSocket 연결 끊겨도 잠시 유지 가능하도록 넉넉하게)
        redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + sessionId, userDto, 24, TimeUnit.HOURS);
        log.debug("세션 저장 완료: sessionId={}, userId={}", sessionId, userDto.getUserId());
    }

    public UserDto getUserSession(String sessionId) {
        Object data = redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + sessionId);
        if (data instanceof UserDto) {
            return (UserDto) data;
        }
        return null;
    }

    public void removeUserSession(String sessionId) {
        redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
        log.debug("세션 삭제 완료: sessionId={}", sessionId);
    }

    // --- Fallback Methods ---

    public UserDto fallbackVerifyUser(String token, Throwable t) {
        log.error("Auth Service 장애 발생: {}", t.getMessage());
        throw new RuntimeException("로그인 서버가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요.");
    }

    public boolean fallbackVerifySubscription(int userId, Long idolId, Throwable t) {
        log.error("Subscription Service 장애 발생: userId={}, idolId={}, error={}", userId, idolId, t.getMessage());
        throw new RuntimeException("구독 정보를 확인할 수 없어 입장이 제한됩니다.");
    }
}
