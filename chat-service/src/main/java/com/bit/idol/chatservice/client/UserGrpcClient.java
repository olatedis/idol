package com.bit.idol.chatservice.client;

import com.bit.idol.chatservice.dto.UserDto;
import com.bit.idol.grpc.UserIdRequest;
import com.bit.idol.grpc.UserResponse;
import com.bit.idol.grpc.UserGrpcServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Service;

@Service
public class UserGrpcClient {

    private final UserGrpcServiceGrpc.UserGrpcServiceBlockingStub userStub;

    public UserGrpcClient() {
        // user-service의 gRPC 포트 (9092로 가정)
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9092)
                .usePlaintext()
                .build();
        this.userStub = UserGrpcServiceGrpc.newBlockingStub(channel);
    }

    public UserDto getUserInfoById(int userId) {
        UserIdRequest request = UserIdRequest.newBuilder()
                .setUserId(userId)
                .build();

        UserResponse response = userStub.getUserById(request);

        return UserDto.builder()
                .userId(response.getUserId())
                .username(response.getUsername())
                .nickname(response.getNickname())
                .build();
    }
}
