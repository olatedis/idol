package com.bit.idol.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // CSRF 비활성화
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/users/**").permitAll() // 회원가입 등 사용자 관련 API 허용
                .requestMatchers("/internal/**").permitAll() // 내부 통신용 API 허용
                .requestMatchers("/actuator/**").permitAll() // 상태 확인용
                .anyRequest().authenticated() // 그 외 요청은 인증 필요
            );
        
        return http.build();
    }
}
