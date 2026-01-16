package com.bit.idol.authservice.client;

import com.bit.idol.authservice.model.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service")
public interface UserFeignClient {
    @GetMapping("/internal/users/info/{username}")
    UserDto getUserInfo(@PathVariable("username") String username);

    @GetMapping("/internal/users/info/id/{userId}") // 경로 수정 필요할 수 있음 (UserService Controller 확인 필요)
    UserDto getUserInfoById(@PathVariable("userId") String userId);

    // 소셜 로그인용 (회원가입/조회)
    @PostMapping("/users/social")
    UserDto registerSocialUser(@RequestBody UserDto userDto);
}
