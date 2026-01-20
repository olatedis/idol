package com.bit.idol.notifyservice.controller;

import com.bit.idol.notifyservice.security.JwtUserIdExtractor;
import com.bit.idol.notifyservice.sse.SseEmitterRegistry;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class SseController {

    private final SseEmitterRegistry registry;
    private final JwtUserIdExtractor userIdExtractor;

    public SseController(SseEmitterRegistry registry, JwtUserIdExtractor userIdExtractor) {
        this.registry = registry;
        this.userIdExtractor = userIdExtractor;
    }

    @GetMapping(value = "/sse/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        int userId = userIdExtractor.extractUserId(jwt);

        // 30분 유지(원하면 늘리기)
        SseEmitter emitter = new SseEmitter(30L * 60L * 1000L);

        registry.add(userId, emitter);

        // 연결 확인용 이벤트 1회
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception ignore) {
        }

        return emitter;
    }
}
