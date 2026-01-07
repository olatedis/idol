package com.bit.idol.authservice.client;

import com.bit.idol.authservice.model.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserFeignClient {
    @GetMapping("/internal/users/info/{username}")
    UserDto getUserInfo(@PathVariable("username") String username);

    @GetMapping("/internal/users/info/{userId}")
    UserDto getUserInfoById(@PathVariable("userId") String userId);
}
