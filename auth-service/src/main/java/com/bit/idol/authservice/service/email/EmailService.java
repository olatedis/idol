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
        String subject = "[DolChat] 회원가입 인증번호 안내";
        String content = String.format(
                """
                        <div style="font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif; background-color: #FFF8DB; padding: 50px 0; width: 100%%; text-align: center;">
                            <div style="background-color: #ffffff; max-width: 500px; margin: 0 auto; border-radius: 16px; padding: 40px 30px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05); text-align: center; border-top: 5px solid #FF9292;">
                                <h1 style="color: #FF9292; font-size: 24px; margin-bottom: 30px; font-weight: bold;">
                                    DolChat 회원가입 인증
                                </h1>
                                <p style="font-size: 16px; color: #555; line-height: 1.6; margin-bottom: 20px;">
                                    안녕하세요!<br>
                                    저희 <b>DolChat</b>에 가입해 주셔서 진심으로 감사합니다.<br>
                                    아래의 <b style="color: #D14D72;">인증 번호</b>를 가입 화면에 입력하여<br>
                                    회원가입 절차를 완료해 주세요.
                                </p>
                                <div style="background-color: #FFF8DB; border-radius: 12px; padding: 20px; display: inline-block; margin: 20px 0;">
                                    <span style="font-size: 32px; font-weight: 800; color: #D14D72; letter-spacing: 5px;">%s</span>
                                </div>
                                <p style="font-size: 13px; color: #999; margin-top: 30px;">
                                    *본 메일은 발신 전용이며, 요청하지 않으셨다면 무시해 주세요.
                                </p>
                            </div>
                        </div>
                        """,
                code);

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
