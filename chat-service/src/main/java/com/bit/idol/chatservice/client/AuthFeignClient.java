package com.bit.idol.chatservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "auth-service")
public interface AuthFeignClient {

    @GetMapping("/auth/verify")
    Map<String, Object> verifyToken(@RequestHeader("Authorization") String token);
}
