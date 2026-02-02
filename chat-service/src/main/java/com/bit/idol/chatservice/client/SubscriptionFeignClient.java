package com.bit.idol.chatservice.client;

import com.bit.idol.chatservice.dto.SubscriptionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "subscription-service")
public interface SubscriptionFeignClient {
    // 구독 여부 확인 (내부 API)
    @GetMapping("/internal/subscriptions/idols/{idolId}/users/{userId}/active")
    boolean checkSubscription(
            @PathVariable("idolId") Long idolId,
            @PathVariable("userId") int userId
    );

    // 내 구독 목록 조회
    @GetMapping("/internal/subscriptions/users/{userId}")
    List<SubscriptionDto> getMySubscriptions(@PathVariable("userId") int userId);
}
