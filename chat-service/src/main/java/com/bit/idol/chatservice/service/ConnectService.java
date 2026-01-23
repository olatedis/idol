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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectService {

    // gRPC 클라이언트 (Auth Service)
    @GrpcClient("auth-service")
    private AuthGrpcServiceGrpc.AuthGrpcServiceBlockingStub authStub;

    private final SubscriptionFeignClient subscriptionFeignClient;

    // 1. 토큰 검증 (Auth Service gRPC 호출)
    @CircuitBreaker(name = "user-check", fallbackMethod = "fallbackVerifyUser")
    public UserDto verifyUser(String token) {
        // gRPC 요청 객체 생성
        TokenRequest request = TokenRequest.newBuilder()
                .setToken(token)
                .build();

        try {
            // gRPC 호출
            AuthResponse response = authStub.verifyToken(request);

            if (!response.getIsValid()) {
                throw new RuntimeException("유효하지 않은 토큰입니다.");
            }

            // DTO 변환
            return UserDto.builder()
                    .userId(Integer.parseInt(response.getUserId()))
                    .nickname(response.getNickname())
                    .role(response.getRole()) // Role 타입 변환 필요 시 수정
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

    // Fallback: 유저 검증 실패 시 (Auth Service 장애)
    public UserDto fallbackVerifyUser(String token, Throwable t) {
        log.error("Auth Service 장애 발생: {}", t.getMessage());
        throw new RuntimeException("로그인 서버가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요.");
    }

    // Fallback: 구독 확인 실패 시 (Subscription Service 장애)
    public boolean fallbackVerifySubscription(int userId, Long idolId, Throwable t) {
        log.error("Subscription Service 장애 발생: userId={}, idolId={}, error={}", userId, idolId, t.getMessage());
        throw new RuntimeException("구독 정보를 확인할 수 없어 입장이 제한됩니다.");
    }
}
