package com.bit.idol.notifyservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // SSE만 JWT 필수
                        .requestMatchers("/sse/**").authenticated()

                        // 기존 REST는 형님 방식(X-User-Id) 유지하려면 일단 열어둠
                        // TODO: 나중에 전체 JWT로 통일할 때 authenticated()로 바꾸면 됨
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt());

        return http.build();
    }
}
