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
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                        .getAddress().getHostAddress()
        );
    }
    
    // (선택) User ID 기반 KeyResolver
    // public KeyResolver userKeyResolver() {
    //     return exchange -> Mono.just(exchange.getRequest().getHeaders().getFirst("X-User-Id"));
    // }
}
