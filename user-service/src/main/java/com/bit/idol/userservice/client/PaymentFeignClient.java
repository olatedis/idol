package com.bit.idol.userservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "payment-service")
public interface PaymentFeignClient {
    // 내 포인트 조회 (API 필요)
    @GetMapping("/internal/payments/users/{userId}/balance")
    int getPointBalance(@PathVariable("userId") int userId);
}
