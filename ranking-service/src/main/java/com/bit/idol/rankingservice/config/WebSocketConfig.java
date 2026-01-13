package com.bit.idol.rankingservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 연결할 엔드포인트: ws://localhost:8085/ws-ranking
        registry.addEndpoint("/ws-ranking")
                .setAllowedOriginPatterns("*") // 모든 도메인 허용 (CORS)
                .withSockJS(); // SockJS 지원
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 구독할 경로 접두사: /topic/votes/{voteId}/ranking
        registry.enableSimpleBroker("/topic");
        
        // 클라이언트가 서버로 메시지 보낼 때 접두사 (지금은 안 씀)
        registry.setApplicationDestinationPrefixes("/app");
    }
}
