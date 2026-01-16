package com.bit.idol.authservice.controller;

import com.bit.idol.authservice.model.LoginRequestDto;
import com.bit.idol.authservice.service.AuthService;
import com.bit.idol.authservice.service.social.SocialAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final SocialAuthService socialAuthService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequestDto request) {
        Map<String, String> tokens = authService.login(request.getUsername(), request.getPassword());
        log.info("로그인 성공: username={}", request.getUsername());
        return ResponseEntity.ok(tokens);
    }

    // 카카오 로그인 API 추가
    @PostMapping("/login/kakao")
    public ResponseEntity<Map<String, String>> loginKakao(@RequestBody Map<String, String> request) {
        String kakaoAccessToken = request.get("token");
        Map<String, String> tokens = socialAuthService.loginKakao(kakaoAccessToken);
        log.info("카카오 로그인 성공");
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        authService.logout(token);
        log.info("로그 아웃 성공");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reissue")
    public ResponseEntity<Map<String, String>> reissue(@RequestHeader("RefreshToken") String refreshToken) {
        Map<String, String> newTokens = authService.reissue(refreshToken);
        log.info("토큰 재발급 성공 (RTR 적용)");
        return ResponseEntity.ok(newTokens);
    }
}
