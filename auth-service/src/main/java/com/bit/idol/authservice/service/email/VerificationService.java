package com.bit.idol.authservice.service.email;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    private static final long CODE_EXPIRATION = 3 * 60 * 1000L; // 3분
    private static final long TOKEN_EXPIRATION = 30 * 60 * 1000L; // 30분 (가입 완료까지 유효)

    // 인증번호 발송
    public void sendCode(String email) {
        // 1. 인증번호 생성 (6자리)
        String code = createCode();

        // 2. Redis 저장 (기존 코드가 있어도 덮어씌움)
        redisTemplate.opsForValue().set(
                "verify:code:" + email,
                code,
                CODE_EXPIRATION,
                TimeUnit.MILLISECONDS
        );

        // 3. 이메일 발송
        emailService.sendVerificationCode(email, code);
    }

    // 인증번호 검증
    public String verifyCode(String email, String code) {
        String storedCode = redisTemplate.opsForValue().get("verify:code:" + email);

        if (storedCode == null) {
            throw new RuntimeException("인증번호가 만료되었거나 존재하지 않습니다.");
        }

        if (!storedCode.equals(code)) {
            throw new RuntimeException("인증번호가 일치하지 않습니다.");
        }

        // 인증 성공 -> 인증 토큰 발급
        String verificationToken = UUID.randomUUID().toString();
        
        // 토큰 저장 (회원가입 API에서 검증용)
        redisTemplate.opsForValue().set(
                "verify:token:" + verificationToken,
                email,
                TOKEN_EXPIRATION,
                TimeUnit.MILLISECONDS
        );
        
        // 사용된 인증번호 삭제
        redisTemplate.delete("verify:code:" + email);

        return verificationToken;
    }

    // 회원가입 시 토큰 검증 (User Service에서 호출할 수도 있고, Auth에서 검증 후 넘길 수도 있음)
    public String getEmailByToken(String token) {
        return redisTemplate.opsForValue().get("verify:token:" + token);
    }

    private String createCode() {
        Random random = new Random();
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            key.append(random.nextInt(10));
        }
        return key.toString();
    }
}
