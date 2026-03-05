package com.bit.idol.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimitConfig {

    // IP 기반으로 Rate Limiting 키 생성
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                        .getAddress().getHostAddress());
    }

    // User ID 기반 KeyResolver
    @Bean
    @Primary
    public KeyResolver userIdKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            // 헤더가 없으면 "anonymous" 버킷으로 합침 (비정상/비로그인 접근)
            return Mono.just(userId != null && !userId.isEmpty() ? userId : "anonymous");
        };
    }
}
