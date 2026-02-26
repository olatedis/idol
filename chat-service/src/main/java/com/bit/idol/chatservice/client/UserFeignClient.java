package com.bit.idol.chatservice.client;

import com.bit.idol.chatservice.dto.IdolDto;
import com.bit.idol.chatservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserFeignClient {
    // 토큰으로 내 정보 조회
    @GetMapping("/users/me")
    UserDto getUserInfo(@RequestHeader("Authorization") String token);

    // 내 아이돌 정보 조회 (X-User-Id 헤더 사용)
    @GetMapping("/idols/me")
    IdolDto getMyIdolInfo(@RequestHeader("X-User-Id") int userId);

    // ID로 유저 정보 조회 (내부 호출용 - 벤치마크 사용)
    @GetMapping("/internal/users/info/id/{userId}")
    UserDto getUserInfoById(@PathVariable("userId") int userId);

    // 유저 신고
    @PostMapping("/internal/users/{userId}/report")
    void reportUser(@PathVariable("userId") int userId);

    // 아이돌 전체 목록 조회
    @GetMapping("/idols")
    List<IdolDto> getAllIdols();

    // 그룹 소속 아이돌 목록 조회 (추가됨)
    @GetMapping("/groups/{groupId}/idols")
    List<IdolDto> getIdolsByGroup(@PathVariable("groupId") int groupId);
}
