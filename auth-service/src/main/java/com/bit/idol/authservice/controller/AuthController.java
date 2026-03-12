package com.bit.idol.authservice.controller;

import com.bit.idol.authservice.client.UserFeignClient;
import com.bit.idol.authservice.dto.response.LoginResponseDto;
import com.bit.idol.authservice.model.LoginRequestDto;
import com.bit.idol.authservice.service.AuthService;
import com.bit.idol.authservice.service.RateLimiterService;
import com.bit.idol.authservice.service.email.PasswordResetService;
import com.bit.idol.authservice.service.email.VerificationService;
import com.bit.idol.authservice.service.social.SocialAuthService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate redisTemplate;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        LoginResponseDto response = authService.login(request.getUsername(), request.getPassword(), clientIp);
        log.info("로그인 성공: username={}", request.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/kakao")
    public ResponseEntity<Map<String, String>> loginKakao(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, String> tokens = socialAuthService.loginKakao(code);
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
    public ResponseEntity<String> sendEmail(@RequestBody Map<String, String> request,
            HttpServletRequest servletRequest) {
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
        return ResponseEntity.ok(token);
    }

    // --- 비밀번호 재설정 API ---

    @PostMapping("/password/reset-request")
    public ResponseEntity<String> requestPasswordReset(@RequestBody Map<String, String> request,
            HttpServletRequest servletRequest) {
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

    @GetMapping("/password/check-token")
    public ResponseEntity<String> checkResetToken(@RequestParam("token") String token) {
        String email = passwordResetService.verifyToken(token);
        return ResponseEntity.ok(email);
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "resetPasswordFallback")
    @PostMapping("/password/reset")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        String email = passwordResetService.verifyToken(token);

        Map<String, String> resetRequest = new HashMap<>();
        resetRequest.put("email", email);
        resetRequest.put("newPassword", newPassword);

        int userId = userFeignClient.resetPassword(resetRequest);

        redisTemplate.delete("RT:" + userId);
        log.info("비밀번호 변경으로 인한 전체 로그아웃 처리: userId={}", userId);

        passwordResetService.deleteToken(token);

        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다. 다시 로그인해주세요.");
    }

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

    // Refresh Token 오류 시 401 반환 처리
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        log.error("Auth Error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    public ResponseEntity<String> resetPasswordFallback(Map<String, String> request, Throwable t) {
        log.error("user-service 통신 장애 (비밀번호 변경 중): {}", t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("현재 비밀번호 변경 서비스를 이용할 수 없습니다. 잠시 후 시도해주세요.");
    }
}
