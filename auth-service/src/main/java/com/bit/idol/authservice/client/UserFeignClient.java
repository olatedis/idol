package com.bit.idol.authservice.client;

import com.bit.idol.authservice.model.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "user-service")
public interface UserFeignClient {
    // 로그인용 유저 정보 조회 (비밀번호 포함)
    @GetMapping("/internal/users/login/{username}")
    UserDto getUserInfo(@PathVariable("username") String username);

    @GetMapping("/internal/users/info/id/{userId}")
    UserDto getUserInfoById(@PathVariable("userId") String userId);

    // 소셜 로그인용 (회원가입/조회)
    @PostMapping("/users/social")
    UserDto registerSocialUser(@RequestBody UserDto userDto);

    // 비밀번호 재설정 (내부 호출용)
    @PostMapping("/internal/users/password/reset")
    int resetPassword(@RequestBody Map<String, String> request);
}
