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
                TimeUnit.MILLISECONDS
        );

        // 3. 이메일 발송
        String resetLink = "http://localhost:3000/reset-password?token=" + token; // 프론트엔드 주소
        String subject = "[Idol Vote] 비밀번호 재설정 안내";
        String content = String.format("""
                <div style="font-family: 'Apple SD Gothic Neo', 'sans-serif' !important; width: 540px; height: 600px; border-top: 4px solid #555; margin: 100px auto; padding: 30px 0; box-sizing: border-box;">
                    <h1 style="margin: 0; padding: 0 5px; font-size: 28px; font-weight: 400;">
                        <span style="font-size: 15px; margin: 0 0 10px 3px;">Idol Vote</span><br />
                        <span style="color: #555;">비밀번호 재설정</span> 안내입니다.
                    </h1>
                    <p style="font-size: 16px; line-height: 26px; margin-top: 50px; padding: 0 5px;">
                        안녕하세요.<br />
                        비밀번호 재설정을 요청하셔서 안내 메일을 보내드립니다.<br />
                        아래 버튼을 클릭하여 새로운 비밀번호를 설정해 주세요.<br />
                        (링크는 10분간 유효합니다.)
                    </p>
                    <a href="%s" style="display: inline-block; margin-top: 30px; padding: 15px 30px; background-color: #555; color: #fff; text-decoration: none; font-size: 16px; border-radius: 5px;">비밀번호 재설정하기</a>
                </div>
                """, resetLink);

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
