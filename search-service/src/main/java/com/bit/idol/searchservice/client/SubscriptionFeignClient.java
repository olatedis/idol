package com.bit.idol.searchservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "subscription-service") // 유레카에 등록된 이름
public interface SubscriptionFeignClient {

    // 구독 여부 확인 API 호출
    @GetMapping("/internal/subscriptions/idols/{idolId}/users/{userId}/active")
    boolean checkSubscription(@PathVariable("userId") int userId, @PathVariable("idolId") Long idolId);
}
