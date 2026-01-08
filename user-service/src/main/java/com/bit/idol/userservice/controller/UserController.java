package com.bit.idol.userservice.controller;

import com.bit.idol.userservice.dto.UserDto;
import com.bit.idol.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.bit.idol.userservice.dto.PasswordChangeDto;
import com.bit.idol.userservice.dto.UserInfoResponse;
import com.bit.idol.userservice.dto.UserUpdateDto;
import com.bit.idol.userservice.dto.UserWithdrawDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserDto userDto) {
        userService.registerUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 완료");
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getUserInfo(@RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role) {
        if (!"USER".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userService.getUserInfo(userId));
    }

    @PostMapping("/me/update")
    public ResponseEntity<String> updateUserInfo(@RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
            @RequestBody UserUpdateDto userUpdateDto) {
        if (!"USER".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("접근 권한이 없습니다.");
        }
        userService.updateUserInfo(userId, userUpdateDto);
        return ResponseEntity.ok("회원정보 수정 완료");
    }

    @PostMapping("/password/change")
    public ResponseEntity<String> changePassword(@RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
            @RequestBody PasswordChangeDto passwordChangeDto) {
        if (!"USER".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("접근 권한이 없습니다.");
        }
        userService.changePassword(userId, passwordChangeDto);
        return ResponseEntity.ok("비밀번호 변경 완료");
    }

    @PostMapping("/withdraw")
    public ResponseEntity<String> withdrawUser(@RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
            @RequestBody UserWithdrawDto userWithdrawDto) {
        if (!"USER".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("접근 권한이 없습니다.");
        }
        userService.withdrawUser(userId, userWithdrawDto.getPassword());
        return ResponseEntity.ok("회원 탈퇴 완료");
    }
}
