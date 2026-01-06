package com.bit.idol.apigateway.security;

import io.jsonwebtoken.Claims;
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

import java.util.List;

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

        if (path.startsWith("/ws/")){
            return  chain.filter(exchange);
        }

        if (HttpMethod.OPTIONS.equals(httpMethod)){
            return chain.filter(exchange);
        }

        for (String w : whiteListPath) {
            if (pathMatcher.match(w, path)) {
                return chain.filter(exchange);
            }
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        Claims claims;

        try {
            claims = jwtTokenProvider.parseClaims(token);
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String userId = claims.getSubject();
        String username = claims.get("username",String.class);
        String nickname = claims.get("nickname",String.class);
        String role = claims.get("role",String.class);

        ServerHttpRequest mutated =
                exchange
                        .getRequest()
                        .mutate()
                        .header("X-User-Id",userId)
                        .header("X-Username",username)
                        .header("X-Nickname",nickname)
                        .header("X-Role",role)
                        .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutated).build();
        return chain.filter(mutatedExchange);

    }

    @Override
    public int getOrder() {
        return -1;
    }

}
