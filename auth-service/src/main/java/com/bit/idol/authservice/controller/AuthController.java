package com.bit.idol.authservice.controller;

import com.bit.idol.authservice.client.UserFeignClient;
import com.bit.idol.authservice.model.LoginRequestDto;
import com.bit.idol.authservice.service.AuthService;
import com.bit.idol.authservice.service.RateLimiterService;
import com.bit.idol.authservice.service.email.PasswordResetService;
import com.bit.idol.authservice.service.email.VerificationService;
import com.bit.idol.authservice.service.social.SocialAuthService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final SocialAuthService socialAuthService;
    private final VerificationService verificationService;
    private final PasswordResetService passwordResetService;
    private final UserFeignClient userFeignClient;
    private final RateLimiterService rateLimiterService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequestDto request) {
        Map<String, String> tokens = authService.login(request.getUsername(), request.getPassword());
        log.info("로그인 성공: username={}", request.getUsername());
        return ResponseEntity.ok(tokens);
    }

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

    // --- 이메일 인증 API ---

    @PostMapping("/email/send")
    public ResponseEntity<String> sendEmail(@RequestBody Map<String, String> request, HttpServletRequest servletRequest) {
        // Rate Limiting 적용
        String clientIp = getClientIp(servletRequest);
        Bucket bucket = rateLimiterService.resolveBucket(clientIp);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("이메일 발송 제한 초과: ip={}, wait={}s", clientIp, waitForRefill);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill))
                    .body("잠시 후 다시 시도해주세요.");
        }

        String email = request.get("email");
        verificationService.sendCode(email);
        return ResponseEntity.ok("인증번호가 발송되었습니다.");
    }

    @PostMapping("/email/verify")
    public ResponseEntity<String> verifyEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        String token = verificationService.verifyCode(email, code);
        return ResponseEntity.ok(token); // 인증 성공 시 토큰 반환
    }

    // --- 비밀번호 재설정 API ---

    // 1. 재설정 링크 발송 요청
    @PostMapping("/password/reset-request")
    public ResponseEntity<String> requestPasswordReset(@RequestBody Map<String, String> request, HttpServletRequest servletRequest) {
        // Rate Limiting 적용 (비밀번호 찾기도 제한)
        String clientIp = getClientIp(servletRequest);
        Bucket bucket = rateLimiterService.resolveBucket(clientIp);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("잠시 후 다시 시도해주세요.");
        }

        String email = request.get("email");
        passwordResetService.sendResetLink(email);
        return ResponseEntity.ok("비밀번호 재설정 링크가 이메일로 발송되었습니다.");
    }

    // 2. 토큰 유효성 확인 (프론트엔드 진입 시 호출)
    @GetMapping("/password/check-token")
    public ResponseEntity<String> checkResetToken(@RequestParam("token") String token) {
        String email = passwordResetService.verifyToken(token);
        return ResponseEntity.ok(email); // 유효하면 이메일 반환
    }

    // 3. 비밀번호 변경 수행
    @PostMapping("/password/reset")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        // 토큰 검증
        String email = passwordResetService.verifyToken(token);

        // User Service 호출하여 비밀번호 변경
        Map<String, String> resetRequest = new HashMap<>();
        resetRequest.put("email", email);
        resetRequest.put("newPassword", newPassword);
        userFeignClient.resetPassword(resetRequest);

        // 토큰 삭제 (재사용 방지)
        passwordResetService.deleteToken(token);

        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    // 클라이언트 IP 추출 유틸 메서드
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
