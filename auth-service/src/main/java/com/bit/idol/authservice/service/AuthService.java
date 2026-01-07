package com.bit.idol.authservice.service;

import com.bit.idol.authservice.client.UserFeignClient;
import com.bit.idol.authservice.model.UserDto;
import com.bit.idol.authservice.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserFeignClient userFeignClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final StringRedisTemplate redisTemplate;

    public Map<String, String> login(String username, String password) {
        // 1. 사용자 정보 조회 (Feign)
        UserDto user = userFeignClient.getUserInfo(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // 2. 비밀번호 검증
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // 3. 토큰 생성
        String userId = String.valueOf(user.getUserId());
        String accessToken = jwtTokenProvider.createAccessToken(userId, user.getUsername(), user.getNickname(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        // 4. Refresh Token Redis 저장
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
        // 1. 토큰 검증 및 파싱
        Claims claims = jwtTokenProvider.parseClaims(token);
        String userId = claims.getSubject();

        // 2. Redis에서 Refresh Token 삭제
        if (redisTemplate.opsForValue().get("RT:" + userId) != null) {
            redisTemplate.delete("RT:" + userId);
        }
    }

    public String reissue(String refreshToken) {
        // 1. Refresh Token 검증
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        String userId = claims.getSubject();

        // 2. Redis에 저장된 Refresh Token 조회
        String storedRefreshToken = redisTemplate.opsForValue().get("RT:" + userId);

        // 3. 토큰 일치 여부 확인
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new RuntimeException("Invalid Refresh Token");
        }

        // 4. 사용자 정보 조회 (Feign)
        UserDto user = userFeignClient.getUserInfoById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // 5. 새로운 Access Token 발급
        return jwtTokenProvider.createAccessToken(
                String.valueOf(user.getUserId()),
                user.getUsername(),
                user.getNickname(),
                user.getRole()
        );
    }
}
