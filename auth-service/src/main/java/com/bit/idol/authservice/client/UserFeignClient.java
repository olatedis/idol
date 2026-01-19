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
    @GetMapping("/internal/users/info/{username}")
    UserDto getUserInfo(@PathVariable("username") String username);

    @GetMapping("/internal/users/info/id/{userId}")
    UserDto getUserInfoById(@PathVariable("userId") String userId);

    // 소셜 로그인용 (회원가입/조회)
    @PostMapping("/users/social")
    UserDto registerSocialUser(@RequestBody UserDto userDto);

    // 비밀번호 재설정 (내부 호출용)
    @PostMapping("/internal/users/password/reset")
    void resetPassword(@RequestBody Map<String, String> request);
}
