package com.bit.idol.userservice.controller;

import com.bit.idol.userservice.dto.UserDto;
import com.bit.idol.userservice.service.UserService;
import jakarta.validation.Valid;
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
@lombok.extern.slf4j.Slf4j
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserDto userDto) {
        log.info("회원가입 요청 받음: username={}", userDto.getUsername());
        userService.registerUser(userDto);
        log.info("회원가입 성공: username={}", userDto.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 완료");
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getUserInfo(@RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role) {
        if (!"USER".equals(role)) {
            log.warn("권한 없는 사용자 접근 시도: userId={}, role={}", userId, role);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("내 정보 조회 요청: userId={}", userId);
        return ResponseEntity.ok(userService.getUserInfo(userId));
    }

    @PostMapping("/me/update")
    public ResponseEntity<String> updateUserInfo(@RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
            @Valid @RequestBody UserUpdateDto userUpdateDto) {
        if (!"USER".equals(role)) {
            log.warn("권한 없는 사용자 접근 시도 (정보 수정): userId={}, role={}", userId, role);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("접근 권한이 없습니다.");
        }
        log.info("회원정보 수정 요청: userId={}", userId);
        userService.updateUserInfo(userId, userUpdateDto);
        log.info("회원정보 수정 완료: userId={}", userId);
        return ResponseEntity.ok("회원정보 수정 완료");
    }

    @PostMapping("/password/change")
    public ResponseEntity<String> changePassword(@RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
            @Valid @RequestBody PasswordChangeDto passwordChangeDto) {
        if (!"USER".equals(role)) {
            log.warn("권한 없는 사용자 접근 시도 (비밀번호 변경): userId={}, role={}", userId, role);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("접근 권한이 없습니다.");
        }
        log.info("비밀번호 변경 요청: userId={}", userId);
        userService.changePassword(userId, passwordChangeDto);
        log.info("비밀번호 변경 완료: userId={}", userId);
        return ResponseEntity.ok("비밀번호 변경 완료");
    }

    @PostMapping("/withdraw")
    public ResponseEntity<String> withdrawUser(@RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
            @Valid @RequestBody UserWithdrawDto userWithdrawDto) {
        if (!"USER".equals(role)) {
            log.warn("권한 없는 사용자 접근 시도 (회원 탈퇴): userId={}, role={}", userId, role);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("접근 권한이 없습니다.");
        }
        log.info("회원 탈퇴 요청: userId={}", userId);
        userService.withdrawUser(userId, userWithdrawDto.getPassword());
        log.info("회원 탈퇴 완료: userId={}", userId);
        return ResponseEntity.ok("회원 탈퇴 완료");
    }
}
