package com.bit.idol.chatservice.client;

import com.bit.idol.chatservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserFeignClient {
    // 토큰으로 내 정보 조회
    @GetMapping("/users/me")
    UserDto getUserInfo(@RequestHeader("Authorization") String token);

    // ID로 유저 정보 조회 (내부 호출용 - 벤치마크 사용)
    @GetMapping("/internal/users/info/id/{userId}")
    UserDto getUserInfoById(@PathVariable("userId") int userId);

    // 유저 신고
    @PostMapping("/internal/users/{userId}/report")
    void reportUser(@PathVariable("userId") int userId);
}
