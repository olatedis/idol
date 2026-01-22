package com.bit.docker.boardservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

// TODO 엔드포인트 맞추기

@FeignClient(name = "agency-service", url = "${clients.agency-service.url}")
public interface AgencyInternalClient {

    // agencyUserId가 idolId를 관리하는지
    @GetMapping("/internal/agencies/users/{agencyUserId}/idols/{idolId}/manageable")
    boolean canManageIdol(@PathVariable("agencyUserId") Integer agencyUserId,
                          @PathVariable("idolId") Long idolId,
                          @RequestHeader("X-Internal-Key") String internalKey);

    // agencyUserId가 groupId를 관리하는지
    @GetMapping("/internal/agencies/users/{agencyUserId}/groups/{groupId}/manageable")
    boolean canManageGroup(@PathVariable("agencyUserId") Integer agencyUserId,
                           @PathVariable("groupId") Long groupId,
                           @RequestHeader("X-Internal-Key") String internalKey);
}
