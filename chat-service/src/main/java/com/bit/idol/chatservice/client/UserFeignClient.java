package com.bit.idol.chatservice.client;

import com.bit.idol.chatservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserFeignClient {
    // 토큰으로 내 정보 조회 (토큰 유효성 검증 겸용)
    @GetMapping("/users/me")
    UserDto getUserInfo(@RequestHeader("Authorization") String token);
}
