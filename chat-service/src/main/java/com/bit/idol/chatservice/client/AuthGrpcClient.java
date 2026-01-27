package com.bit.idol.chatservice.client;

import com.bit.idol.grpc.AuthGrpcServiceGrpc;
import com.bit.idol.grpc.AuthResponse;
import com.bit.idol.grpc.TokenRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthGrpcClient {

    private final AuthGrpcServiceGrpc.AuthGrpcServiceBlockingStub authStub;

    public AuthGrpcClient() {
        // auth-service의 gRPC 포트 (9091)
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9091)
                .usePlaintext()
                .build();
        this.authStub = AuthGrpcServiceGrpc.newBlockingStub(channel);
    }

    public Map<String, Object> verifyToken(String token) {
        TokenRequest request = TokenRequest.newBuilder()
                .setToken(token)
                .build();

        AuthResponse response = authStub.verifyToken(request);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", response.getUserId());
        result.put("nickname", response.getNickname());
        result.put("role", response.getRole());
        result.put("isValid", response.getIsValid());
        return result;
    }
}
