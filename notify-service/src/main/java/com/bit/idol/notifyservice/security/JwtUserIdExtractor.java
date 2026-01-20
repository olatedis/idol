package com.bit.idol.notifyservice.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtUserIdExtractor {

    public int extractUserId(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null) {
            throw new IllegalArgumentException("JWT에 sub(userId)가 없습니다.");
        }

        try {
            return Integer.parseInt(sub);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("JWT sub를 userId로 변환할 수 없습니다.");
        }
    }
}
