package com.bit.idol.chatservice.config;

import com.bit.idol.chatservice.handler.StompHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompHandler stompHandler;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트 연결 엔드포인트: ws://localhost:8089/ws-chat
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*") // 모든 도메인 허용 (보안상 나중에 구체화 필요)
                .withSockJS(); // SockJS 지원 (선택 사항, 웹소켓 미지원 브라우저 대비)
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지 구독 요청 prefix (클라이언트가 메시지 받을 때)
        // /sub/idol/{id} -> 아이돌 메시지 수신 (Broadcasting)
        // /queue/idol/{id} -> 아이돌이 팬 메시지 수신 (Unicasting)
        registry.enableSimpleBroker("/sub", "/queue");

        // 메시지 발행 요청 prefix (클라이언트가 메시지 보낼 때)
        // /pub/chat/send
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 연결 시 토큰 검증 및 구독 권한 체크 인터셉터 등록
        registration.interceptors(stompHandler);
    }
}
