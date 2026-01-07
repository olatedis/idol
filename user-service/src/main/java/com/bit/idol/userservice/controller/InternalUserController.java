package com.bit.idol.userservice.controller;

import com.bit.idol.userservice.dto.UserDto;
import com.bit.idol.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {
    private final UserService userService;

    @GetMapping("/login-info/{username}")
    public ResponseEntity<UserDto> getUserInfo(@PathVariable("username") String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @GetMapping("/info/{userId}")
    public ResponseEntity<UserDto> getUserInfoById(@PathVariable("userId") int userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }
}
