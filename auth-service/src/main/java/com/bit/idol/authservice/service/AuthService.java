package com.bit.idol.authservice.service;

import com.bit.idol.authservice.client.UserFeignClient;
import com.bit.idol.authservice.model.UserDto;
import com.bit.idol.authservice.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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

    public Map<String, String> login(String username, String password) {
        // 1. 사용자 정보 조회 (Feign)
        UserDto user = userFeignClient.getUserInfo(username);
        
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        // 2. 차단 여부 확인 (BANNED 상태면 로그인 불가)
        if ("BANNED".equals(user.getStatus())) {
            throw new RuntimeException("계정이 정지되었습니다.");
        }

        // 3. 비밀번호 검증
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // 4. 토큰 생성
        String userId = String.valueOf(user.getUserId());
        String accessToken = jwtTokenProvider.createAccessToken(userId, user.getUsername(), user.getNickname(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        // 5. Refresh Token Redis 저장
        redisTemplate.opsForValue().set(
                "RT:" + userId,
                refreshToken,
                jwtTokenProvider.getRefreshTokenValidity(),
                TimeUnit.MILLISECONDS
        );

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        
        return tokens;
    }

    public void logout(String token) {
        String userId;
        try {
            // 1. 토큰 검증 및 파싱
            Claims claims = jwtTokenProvider.parseClaims(token);
            userId = claims.getSubject();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이어도 로그아웃 진행 (Claims 복구)
            userId = e.getClaims().getSubject();
        }

        // 2. Redis에서 Refresh Token 삭제
        if (redisTemplate.opsForValue().get("RT:" + userId) != null) {
            redisTemplate.delete("RT:" + userId);
        }
    }

    // Refresh Token Rotation (RTR) 적용
    public Map<String, String> reissue(String refreshToken) {
        // 1. Refresh Token 검증
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        String userId = claims.getSubject();

        // 2. Redis에 저장된 Refresh Token 조회
        String storedRefreshToken = redisTemplate.opsForValue().get("RT:" + userId);

        // 3. 토큰 일치 여부 확인
        if (storedRefreshToken == null) {
            // 이미 로그아웃되었거나 만료된 경우
            throw new RuntimeException("Refresh Token이 만료되었거나 존재하지 않습니다.");
        }

        if (!storedRefreshToken.equals(refreshToken)) {
            // ★ 토큰 탈취 감지! (저장된 것과 다른 토큰으로 요청함)
            log.warn("토큰 탈취 감지! userId={}", userId);
            // 해당 유저의 모든 Refresh Token 삭제 (강제 로그아웃)
            redisTemplate.delete("RT:" + userId);
            throw new RuntimeException("유효하지 않은 Refresh Token입니다. (토큰 탈취 의심)");
        }

        // 4. 사용자 정보 조회 (Feign)
        UserDto user = userFeignClient.getUserInfoById(userId);
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        // 5. 차단 여부 확인 (재발급 시에도 체크)
        if ("BANNED".equals(user.getStatus())) {
            // 차단된 유저라면 Refresh Token 삭제 후 예외 발생
            redisTemplate.delete("RT:" + userId);
            throw new RuntimeException("계정이 정지되었습니다.");
        }

        // 6. 새로운 토큰 쌍 발급 (Access + Refresh)
        String newAccessToken = jwtTokenProvider.createAccessToken(
                String.valueOf(user.getUserId()),
                user.getUsername(),
                user.getNickname(),
                user.getRole()
        );
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        // 7. Redis 업데이트 (RTR)
        redisTemplate.opsForValue().set(
                "RT:" + userId,
                newRefreshToken,
                jwtTokenProvider.getRefreshTokenValidity(),
                TimeUnit.MILLISECONDS
        );

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshToken);

        return tokens;
    }
}
