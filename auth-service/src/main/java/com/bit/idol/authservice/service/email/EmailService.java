package com.bit.idol.authservice.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    public void sendVerificationCode(String toEmail, String code) {
        String subject = "[Idol Vote] 회원가입 인증번호 안내";
        String content = String.format("""
                <div style="font-family: 'Apple SD Gothic Neo', 'sans-serif' !important; width: 540px; height: 600px; border-top: 4px solid #555; margin: 100px auto; padding: 30px 0; box-sizing: border-box;">
                    <h1 style="margin: 0; padding: 0 5px; font-size: 28px; font-weight: 400;">
                        <span style="font-size: 15px; margin: 0 0 10px 3px;">Idol Vote</span><br />
                        <span style="color: #555;">메일인증</span> 안내입니다.
                    </h1>
                    <p style="font-size: 16px; line-height: 26px; margin-top: 50px; padding: 0 5px;">
                        안녕하세요.<br />
                        Idol Vote에 가입해 주셔서 진심으로 감사드립니다.<br />
                        아래 <b style="color: #555;">'인증 번호'</b>를 입력하여 회원가입을 완료해 주세요.<br />
                        감사합니다.
                    </p>
                    <p style="font-size: 16px; margin: 40px 5px 20px; line-height: 28px;">
                        인증 번호: <br />
                        <span style="font-size: 24px; font-weight: bold; color: #555;">%s</span>
                    </p>
                </div>
                """, code);

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true); // true: HTML 모드

            javaMailSender.send(message);
            log.info("이메일 발송 성공: {}", toEmail);
        } catch (MessagingException e) {
            log.error("이메일 발송 실패: {}", e.getMessage());
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다.");
        }
    }
}
