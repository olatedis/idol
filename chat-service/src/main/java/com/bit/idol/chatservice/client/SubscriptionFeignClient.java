package com.bit.idol.chatservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "subscription-service")
public interface SubscriptionFeignClient {
    // 구독 여부 확인 (내부 API)
    @GetMapping("/subscriptions/internal/check")
    boolean checkSubscription(
            @RequestHeader("X-User-Id") int userId,
            @RequestParam("idolId") Long idolId
    );
}
