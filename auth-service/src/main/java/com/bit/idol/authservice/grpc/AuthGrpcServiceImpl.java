package com.bit.idol.authservice.grpc;

import com.bit.idol.authservice.security.JwtTokenProvider;
import com.bit.idol.grpc.AuthGrpcServiceGrpc;
import com.bit.idol.grpc.AuthResponse;
import com.bit.idol.grpc.TokenRequest;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AuthGrpcServiceImpl extends AuthGrpcServiceGrpc.AuthGrpcServiceImplBase {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void verifyToken(TokenRequest request, StreamObserver<AuthResponse> responseObserver) {
        String token = request.getToken();
        
        // Bearer 제거
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            // 1. 토큰 파싱 및 검증
            Claims claims = jwtTokenProvider.parseClaims(token);

            // 2. 정보 추출
            String userId = claims.getSubject();
            String nickname = claims.get("nickname", String.class);
            String role = claims.get("role", String.class);

            // 3. 응답 생성
            AuthResponse response = AuthResponse.newBuilder()
                    .setUserId(userId)
                    .setNickname(nickname != null ? nickname : "")
                    .setRole(role != null ? role : "")
                    .setIsValid(true)
                    .build();

            // 4. 전송
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC 토큰 검증 실패: {}", e.getMessage());
            
            // 실패 응답 (빈 값 + isValid=false)
            AuthResponse response = AuthResponse.newBuilder()
                    .setIsValid(false)
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
