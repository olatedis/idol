package com.bit.idol.userservice.controller;

import com.bit.idol.userservice.dto.user.PasswordChangeDto;
import com.bit.idol.userservice.dto.user.UserDto;
import com.bit.idol.userservice.dto.user.UserInfoResponse;
import com.bit.idol.userservice.dto.user.UserUpdateDto;
import com.bit.idol.userservice.dto.user.UserWithdrawDto;
import com.bit.idol.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserDto userDto) {
        log.info("회원가입 요청 받음: username={}", userDto.getUsername());
        userService.registerUser(userDto);
        log.info("회원가입 성공: username={}", userDto.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 완료");
    }

    // 소셜 로그인용 회원가입/조회 API (Auth Service에서 호출)
    @PostMapping("/social")
    public ResponseEntity<UserDto> registerSocialUser(@RequestBody UserDto userDto) {
        log.info("소셜 회원가입/조회 요청: provider={}, providerId={}", userDto.getProvider(), userDto.getProviderId());
        UserDto result = userService.registerSocialUser(userDto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getUserInfo(@RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role) {
        if (!"USER".equals(role)) {
            log.warn("권한 없는 사용자 접근 시도: userId={}, role={}", userId, role);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("내 정보 조회 요청: userId={}", userId);
        return ResponseEntity.ok(userService.getUserById(userId));
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

    // 프로필 이미지 업로드 API
    @PostMapping("/me/image")
    public ResponseEntity<String> updateProfileImage(@RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
            @RequestParam("file") MultipartFile file) {
        if (!"USER".equals(role)) {
            log.warn("권한 없는 사용자 접근 시도 (이미지 업로드): userId={}, role={}", userId, role);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("접근 권한이 없습니다.");
        }
        log.info("프로필 이미지 변경 요청: userId={}", userId);
        
        // UserService가 UserDto를 반환하도록 변경되었으므로 수정
        UserDto updatedUser = userService.updateProfileImage(userId, file);
        String fileUrl = updatedUser.getImgUrl();

        log.info("프로필 이미지 변경 완료: userId={}, url={}", userId, fileUrl);
        return ResponseEntity.ok(fileUrl);
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
