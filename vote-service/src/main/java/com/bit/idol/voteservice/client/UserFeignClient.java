package com.bit.idol.voteservice.client;

import com.bit.idol.voteservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserFeignClient {
    @GetMapping("/internal/users/info/id/{userId}")
    UserDto getUserInfoById(@PathVariable("userId") int userId);
}
