package com.bit.idol.userservice.controller;

import com.bit.idol.userservice.dto.user.UserDto;
import com.bit.idol.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Slf4j
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/info/{username}")
    public ResponseEntity<UserDto> getUserInfo(@PathVariable("username") String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @GetMapping("/info/id/{userId}")
    public ResponseEntity<UserDto> getUserInfoById(@PathVariable("userId") int userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    // Fanout Service용 전체 유저 조회
    @GetMapping("/info/all")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // 비밀번호 재설정 (Auth Service에서 호출)
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");
        
        log.info("비밀번호 재설정 요청 (Internal): email={}", email);
        userService.resetPassword(email, newPassword);
        
        return ResponseEntity.ok().build();
    }
}
