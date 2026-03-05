package com.bit.idol.authservice.service.email;

import com.bit.idol.authservice.client.UserFeignClient;
import com.bit.idol.authservice.model.UserDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender javaMailSender;
    private final UserFeignClient userFeignClient; // 유저 존재 여부 확인용 (선택)

    private static final long RESET_TOKEN_EXPIRATION = 10 * 60 * 1000L; // 10분

    // 비밀번호 재설정 요청 (링크 발송)
    public void sendResetLink(String email) {
        // 1. 토큰 생성
        String token = UUID.randomUUID().toString();

        // 2. Redis 저장 (key: token, value: email)
        redisTemplate.opsForValue().set(
                "reset:token:" + token,
                email,
                RESET_TOKEN_EXPIRATION,
                TimeUnit.MILLISECONDS);

        // 3. 이메일 발송
        String resetLink = "http://localhost:3000/reset-password?token=" + token; // 프론트엔드 주소
        String subject = "[Idol] 비밀번호 재설정 안내";
        String content = String.format(
                """
                        <div style="font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif; background-color: #FFF8DB; padding: 50px 0; width: 100%%; text-align: center;">
                            <div style="background-color: #ffffff; max-width: 500px; margin: 0 auto; border-radius: 16px; padding: 40px 30px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05); text-align: center; border-top: 5px solid #FF9292;">
                                <h1 style="color: #FF9292; font-size: 24px; margin-bottom: 30px; font-weight: bold;">
                                    비밀번호 재설정 안내
                                </h1>
                                <p style="font-size: 16px; color: #555; line-height: 1.6; margin-bottom: 20px;">
                                    안녕하세요.<br>
                                    비밀번호를 새롭게 설정하기 위해 발송된 메일입니다.<br>
                                    아래 버튼을 클릭하여 안전하게 새로운 비밀번호를<br>
                                    변경해 주시길 바랍니다.
                                </p>
                                <a href="%s" style="display: inline-block; margin-top: 20px; padding: 16px 32px; background-color: #FF9292; color: #ffffff; text-decoration: none; font-size: 16px; font-weight: bold; border-radius: 12px; box-shadow: 0 4px 6px rgba(255, 146, 146, 0.3);">
                                    비밀번호 재설정하기
                                </a>
                                <p style="font-size: 13px; color: #999; margin-top: 30px;">
                                    *(해당 링크는 발송 후 10분 동안만 유효합니다.)<br>
                                    *본인이 요청하지 않으셨다면 이 메일을 무시해 주세요.
                                </p>
                            </div>
                        </div>
                        """,
                resetLink);

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(content, true);

            javaMailSender.send(message);
            log.info("비밀번호 재설정 메일 발송 성공: {}", email);
        } catch (MessagingException e) {
            log.error("메일 발송 실패: {}", e.getMessage());
            throw new RuntimeException("메일 발송 중 오류가 발생했습니다.");
        }
    }

    // 토큰 검증 및 이메일 반환
    public String verifyToken(String token) {
        String email = redisTemplate.opsForValue().get("reset:token:" + token);
        if (email == null) {
            throw new RuntimeException("유효하지 않거나 만료된 링크입니다.");
        }
        return email;
    }

    // 비밀번호 변경 완료 후 토큰 삭제
    public void deleteToken(String token) {
        redisTemplate.delete("reset:token:" + token);
    }
}
