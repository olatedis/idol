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
        UserDto user = userFeignClient.getUserInfo(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if ("BANNED".equals(user.getStatus())) {
            throw new RuntimeException("Your account has been banned.");
        }

        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String userId = String.valueOf(user.getUserId());
        String accessToken = jwtTokenProvider.createAccessToken(userId, user.getUsername(), user.getNickname(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

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
            Claims claims = jwtTokenProvider.parseClaims(token);
            userId = claims.getSubject();
        } catch (ExpiredJwtException e) {
            userId = e.getClaims().getSubject();
        }

        if (redisTemplate.opsForValue().get("RT:" + userId) != null) {
            redisTemplate.delete("RT:" + userId);
        }
    }

    public Map<String, String> reissue(String refreshToken) {
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        String userId = claims.getSubject();

        String storedRefreshToken = redisTemplate.opsForValue().get("RT:" + userId);

        if (storedRefreshToken == null) {
            throw new RuntimeException("Refresh Token expired or not found");
        }

        if (!storedRefreshToken.equals(refreshToken)) {
            log.warn("토큰 탈취 감지! userId={}", userId);
            redisTemplate.delete("RT:" + userId);
            throw new RuntimeException("Invalid Refresh Token (Token Theft Detected)");
        }

        UserDto user = userFeignClient.getUserInfoById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if ("BANNED".equals(user.getStatus())) {
            redisTemplate.delete("RT:" + userId);
            throw new RuntimeException("Your account has been banned.");
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(
                String.valueOf(user.getUserId()),
                user.getUsername(),
                user.getNickname(),
                user.getRole()
        );
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

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

    // 토큰 검증 메서드 (벤치마크용)
    public Map<String, Object> verifyToken(String token) {
        Claims claims = jwtTokenProvider.parseClaims(token);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", claims.getSubject());
        result.put("username", claims.get("username"));
        result.put("nickname", claims.get("nickname"));
        result.put("role", claims.get("role"));
        result.put("isValid", true);
        return result;
    }
}
