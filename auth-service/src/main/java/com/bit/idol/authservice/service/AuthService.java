package com.bit.idol.authservice.service;

import com.bit.idol.authservice.client.UserFeignClient;
import com.bit.idol.authservice.dto.response.LoginResponseDto;
import com.bit.idol.authservice.model.UserDto;
import com.bit.idol.authservice.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserFeignClient userFeignClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final StringRedisTemplate redisTemplate;

    @CircuitBreaker(name = "user-service", fallbackMethod = "loginFallback")
    public LoginResponseDto login(String username, String password) {
        // 1. 사용자 정보 조회 (Feign)
        UserDto user = userFeignClient.getUserInfo(username);

        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        // 2. 차단 여부 확인
        if ("BANNED".equals(user.getStatus())) {
            throw new RuntimeException("계정이 정지되었습니다.");
        }

        // 3. 브루트 포스 방어 (Redis 기반 실패 횟수 잠금)
        String failKey = "login:fail:" + username;
        String failCountStr = redisTemplate.opsForValue().get(failKey);
        int failCount = failCountStr != null ? Integer.parseInt(failCountStr) : 0;

        if (failCount >= 5) {
            throw new RuntimeException("비밀번호 5회 오류로 계정이 30분간 잠겼습니다.");
        }

        // 4. 비밀번호 검증
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            // 실패 시 카운트 증가 및 30분 제한 설정
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, 30, java.util.concurrent.TimeUnit.MINUTES);
            int newFailCount = failCount + 1;
            throw new RuntimeException("비밀번호가 일치하지 않습니다. (실패 " + newFailCount + "회/5회 남음)");
        }

        // 5. 성공 시 카운트 초기화
        redisTemplate.delete(failKey);

        // 4. 토큰 생성
        String userId = String.valueOf(user.getUserId());
        String accessToken = jwtTokenProvider.createAccessToken(userId, user.getUsername(), user.getNickname(),
                user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        // 5. Refresh Token Redis 저장
        redisTemplate.opsForValue().set(
                "RT:" + userId,
                refreshToken,
                jwtTokenProvider.getRefreshTokenValidity(),
                TimeUnit.MILLISECONDS);

        // 비밀번호는 보안상 제거 후 반환
        user.setPassword(null);

        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(user)
                .build();
    }

    public void logout(String token) {
        String userId;
        try {
            Claims claims = jwtTokenProvider.parseClaims(token);
            userId = claims.getSubject();
        } catch (ExpiredJwtException e) {
            userId = e.getClaims().getSubject();
        }

        if (redisTemplate.opsForValue().get("RT:" + userId) != null) {
            redisTemplate.delete("RT:" + userId);
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "reissueFallback")
    public Map<String, String> reissue(String refreshToken) {
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        String userId = claims.getSubject();

        String storedRefreshToken = redisTemplate.opsForValue().get("RT:" + userId);

        if (storedRefreshToken == null) {
            throw new RuntimeException("Refresh Token이 만료되었거나 존재하지 않습니다.");
        }

        if (!storedRefreshToken.equals(refreshToken)) {
            log.warn("토큰 탈취 감지! userId={}", userId);
            redisTemplate.delete("RT:" + userId);
            throw new RuntimeException("유효하지 않은 Refresh Token입니다.");
        }

        UserDto user = userFeignClient.getUserInfoById(userId);
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        if ("BANNED".equals(user.getStatus())) {
            redisTemplate.delete("RT:" + userId);
            throw new RuntimeException("계정이 정지되었습니다.");
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(
                String.valueOf(user.getUserId()),
                user.getUsername(),
                user.getNickname(),
                user.getRole());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        redisTemplate.opsForValue().set(
                "RT:" + userId,
                newRefreshToken,
                jwtTokenProvider.getRefreshTokenValidity(),
                TimeUnit.MILLISECONDS);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshToken);

        return tokens;
    }

    public LoginResponseDto loginFallback(String username, String password, Throwable t) {
        log.error("user-service 통신 장애 (로그인 시도 중): {}", t.getMessage());
        throw new RuntimeException("현재 로그인 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }

    public Map<String, String> reissueFallback(String refreshToken, Throwable t) {
        log.error("user-service 통신 장애 (토큰 재발급 시도 중): {}", t.getMessage());
        throw new RuntimeException("현재 토큰 재발급 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
}
