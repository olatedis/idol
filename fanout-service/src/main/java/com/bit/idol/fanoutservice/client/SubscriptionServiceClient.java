package com.bit.idol.fanoutservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

// subscription-service internal API 호출
@FeignClient(name = "subscription-service", url = "${clients.subscription-service.url}")
public interface SubscriptionServiceClient {

    @GetMapping("/internal/subscriptions/idols/{idolId}/user-ids")
    List<Integer> getIdolSubscriberUserIds(@PathVariable("idolId") Long idolId);

    @GetMapping("/internal/subscriptions/groups/{groupId}/user-ids")
    List<Integer> getGroupSubscriberUserIds(@PathVariable("groupId") Long groupId);
}
