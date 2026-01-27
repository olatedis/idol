package com.bit.docker.boardservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "subscription-service", url = "${clients.subscription-service.url}")
public interface SubscriptionInternalClient {

    // idol 유료 구독 active 여부
    @GetMapping("/internal/subscriptions/idols/{idolId}/users/{userId}/active")
    boolean isActiveIdolSubscriber(
            @PathVariable("idolId") Long idolId,
            @PathVariable("userId") Integer userId
    );

    // group 구독 active 여부
    @GetMapping("/internal/subscriptions/groups/{groupId}/users/{userId}/active")
    boolean isActiveGroupSubscriber(
            @PathVariable("groupId") Long groupId,
            @PathVariable("userId") Integer userId
    );
}
