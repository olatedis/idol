package com.bit.idol.userservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "subscription-service")
public interface SubscriptionFeignClient {
    // 내 구독 개수 조회 (API 필요)
    @GetMapping("/internal/subscriptions/users/{userId}/count")
    int getMySubscriptionCount(@PathVariable("userId") int userId);
}
