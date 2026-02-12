package com.bit.idol.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .cors(ServerHttpSecurity.CorsSpec::disable)
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // URL에 "/internal/"이 포함되어 있으면 무조건 차단
                .matchers(exchange -> {
                    if (exchange.getRequest().getPath().value().contains("/internal/")) {
                        return ServerWebExchangeMatcher.MatchResult.match();
                    } else {
                        return ServerWebExchangeMatcher.MatchResult.notMatch();
                    }
                }).denyAll()

                .anyExchange().permitAll() // 나머지 요청 허용 (인증은 JwtAuthenticationFilter에서 처리)
            );
        return http.build();
    }
}
