package com.bit.idol.apigateway.security;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private final JwtTokenProvider jwtTokenProvider;
    private final List<String> whiteListPath;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, @Value("${jwt.white-list-path}") String[] whiteListPath) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.whiteListPath = List.of(whiteListPath);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod httpMethod = exchange.getRequest().getMethod();
        
        log.info("Incoming request: {} {}", httpMethod, path);

        if (path.startsWith("/ws/")){
            return  chain.filter(exchange);
        }

        if (HttpMethod.OPTIONS.equals(httpMethod)){
            return chain.filter(exchange);
        }

        for (String w : whiteListPath) {
            if (pathMatcher.match(w, path)) {
                log.info("Whitelisted path: {}", path);
                return chain.filter(exchange);
            }
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        Claims claims;

        try {
            claims = jwtTokenProvider.parseClaims(token);
        } catch (Exception e) {
            log.error("Invalid token: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String userId = claims.getSubject();
        String username = claims.get("username",String.class);
        String nickname = claims.get("nickname",String.class);
        String role = claims.get("role",String.class);
        
        log.info("User authenticated: userId={}, username={}", userId, username);

        ServerHttpRequest.Builder builder = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-Role", role);
                
        if (username != null) {
            builder.header("X-Username", username);
        }
        
        if (nickname != null) {
            // 한글 닉네임 인코딩 처리
            builder.header("X-Nickname", URLEncoder.encode(nickname, StandardCharsets.UTF_8));
        }

        ServerWebExchange mutatedExchange = exchange.mutate().request(builder.build()).build();
        return chain.filter(mutatedExchange);

    }

    @Override
    public int getOrder() {
        return -1;
    }

}
