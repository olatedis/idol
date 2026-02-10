package com.bit.idol.authservice.service.social;

import com.bit.idol.authservice.client.UserFeignClient;
import com.bit.idol.authservice.model.Role;
import com.bit.idol.authservice.model.UserDto;
import com.bit.idol.authservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialAuthService {

    private final KakaoAuthClient kakaoAuthClient;
    private final UserFeignClient userFeignClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    public Map<String, String> loginKakao(String code) {
        // 1. 인가 코드로 Access Token 발급받기 (Server-side)
        String kakaoAccessToken = kakaoAuthClient.getToken(code);

        // 2. 카카오 API 호출하여 유저 정보 가져오기
        Map<String, Object> kakaoInfo = kakaoAuthClient.getUserInfo(kakaoAccessToken);
        
        // 3. 데이터 파싱
        String providerId = String.valueOf(kakaoInfo.get("id"));
        Map<String, Object> kakaoAccount = (Map<String, Object>) kakaoInfo.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String nickname = (String) profile.get("nickname");
        String imgUrl = (String) profile.get("profile_image_url");
        String email = (String) kakaoAccount.get("email"); // 이메일 동의 안 하면 null일 수 있음

        // 4. UserDto 생성
        UserDto userDto = UserDto.builder()
                .provider("KAKAO")
                .providerId(providerId)
                .nickname(nickname)
                .email(email != null ? email : providerId + "@kakao.com") // 이메일 없으면 임시 생성
                .imgUrl(imgUrl)
                .role(Role.USER)
                .build();

        // 5. User Service 호출 (회원가입 or 조회)
        UserDto savedUser = userFeignClient.registerSocialUser(userDto);

        // 6. JWT 토큰 발급
        String userId = String.valueOf(savedUser.getUserId());
        String accessToken = jwtTokenProvider.createAccessToken(userId, savedUser.getUsername(), savedUser.getNickname(), savedUser.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        // 7. Refresh Token Redis 저장
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
}
