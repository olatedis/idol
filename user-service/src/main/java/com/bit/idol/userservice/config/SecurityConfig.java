package com.bit.idol.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // CSRF 비활성화
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 사용 안 함 (최적화)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/users/**", "/idols/**").permitAll() // 회원가입, 아이돌 조회 등 허용
                .requestMatchers("/internal/**").permitAll() // 내부 통신용 API 허용
                .requestMatchers("/actuator/**").permitAll() // 상태 확인용
                // Swagger 관련 경로 허용
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated() // 그 외 요청은 인증 필요
            );
        
        return http.build();
    }
}
