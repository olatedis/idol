package com.bit.docker.boardservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${clients.user-service.url}")
public interface UserInternalClient {

    // Idol 본인 검증: userId가 idolId의 owner냐
    @GetMapping("/internal/users/idols/{idolId}/owners/{userId}")
    boolean isIdolOwner(
            @PathVariable("idolId") Long idolId,
            @PathVariable("userId") Integer userId
    );

    // Agency가 idol 관리 가능한지
    @GetMapping("/internal/users/agencies/users/{agencyUserId}/idols/{idolId}/manageable")
    boolean canAgencyManageIdol(
            @PathVariable("agencyUserId") Integer agencyUserId,
            @PathVariable("idolId") Long idolId
    );

    // Group 멤버 여부 (그룹 엔티티 만든 뒤 user-service에서 구현)
    @GetMapping("/internal/users/groups/{groupId}/members/{userId}/exists")
    boolean isGroupMember(
            @PathVariable("groupId") Long groupId,
            @PathVariable("userId") Integer userId
    );

    // Agency가 group 관리 가능한지 (그룹 엔티티 만든 뒤 user-service에서 구현)
    @GetMapping("/internal/users/agencies/users/{agencyUserId}/groups/{groupId}/manageable")
    boolean canAgencyManageGroup(
            @PathVariable("agencyUserId") Integer agencyUserId,
            @PathVariable("groupId") Long groupId
    );
}
