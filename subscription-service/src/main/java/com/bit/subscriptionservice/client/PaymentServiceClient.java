package com.bit.subscriptionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "payment-service")
public interface PaymentServiceClient {

    @PostMapping("/payments/internal/billing-complete")
    void billingComplete(@RequestBody Map<String, Object> request);
}
