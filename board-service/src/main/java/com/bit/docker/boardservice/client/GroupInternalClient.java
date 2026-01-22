package com.bit.docker.boardservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

// TODO 엔드포인트 맞추기

@FeignClient(name = "group-service", url = "${clients.group-service.url}")
public interface GroupInternalClient {

    // userId(IDOL)가 groupId 멤버인지 확인
    @GetMapping("/internal/groups/{groupId}/members/{userId}/exists")
    boolean isMember(@PathVariable("groupId") Long groupId,
                     @PathVariable("userId") Integer userId,
                     @RequestHeader("X-Internal-Key") String internalKey);
}
