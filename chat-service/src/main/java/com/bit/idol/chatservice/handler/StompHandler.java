package com.bit.idol.chatservice.handler;

import com.bit.idol.chatservice.dto.UserDto;
import com.bit.idol.chatservice.service.ChatService;
import com.bit.idol.chatservice.service.ConnectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final ConnectService connectService;
    private final ChatService chatService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && accessor.getCommand() != null) {
            switch (accessor.getCommand()) {
                case CONNECT:
                    handleConnect(accessor);
                    break;
                case SEND:
                    handleSend(accessor);
                    break;
                case DISCONNECT:
                    handleDisconnect(accessor);
                    break;
                default:
                    break;
            }
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        // 1. 토큰 검증 (gRPC + 서킷 브레이커)
        String token = accessor.getFirstNativeHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("인증 토큰이 없습니다.");
        }

        UserDto user = connectService.verifyUser(token);

        // 2. 구독 여부 확인 (USER인 경우만)
        if ("USER".equals(user.getRole())) {
            String idolIdStr = accessor.getFirstNativeHeader("idolId");
            if (idolIdStr == null) {
                throw new RuntimeException("idolId 헤더가 필요합니다.");
            }

            Long idolId = Long.parseLong(idolIdStr);
            boolean isSubscribed = connectService.verifySubscription(user.getUserId(), idolId);
            
            if (!isSubscribed) {
                throw new RuntimeException("구독하지 않은 아이돌입니다.");
            }
        }
        // 3. 아이돌인 경우 접속 상태 ON
        else if ("IDOL".equals(user.getRole())) {
            chatService.setIdolOnline((long) user.getUserId(), true);
            log.info("아이돌 접속 ON: idolId={}", user.getUserId());
        }

        // 4. Redis에 세션 정보 저장 (캐싱)
        connectService.saveUserSession(accessor.getSessionId(), user);
        
        // 5. 세션 속성에도 저장 (메모리 캐시 - 이중 안전장치)
        accessor.getSessionAttributes().put("userId", user.getUserId());
        accessor.getSessionAttributes().put("role", user.getRole());
        accessor.getSessionAttributes().put("nickname", user.getNickname());
        
        log.info("웹소켓 연결 성공: userId={}, sessionId={}", user.getUserId(), accessor.getSessionId());
    }

    private void handleSend(StompHeaderAccessor accessor) {
        // Redis에서 세션 정보 확인 (외부 서버 호출 X)
        UserDto user = connectService.getUserSession(accessor.getSessionId());
        
        if (user == null) {
            // Redis에 없으면 메모리(SessionAttributes) 확인 (Fallback)
            Integer userId = (Integer) accessor.getSessionAttributes().get("userId");
            if (userId == null) {
                throw new RuntimeException("세션이 만료되었습니다. 다시 로그인해주세요.");
            }
            // 메모리엔 있는데 Redis엔 없으면 다시 저장 (복구)
            // (여기서는 생략)
        }
        
        // 추가 검증 로직이 필요하다면 여기서 수행 (예: 도배 방지 등)
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        
        // Redis 세션 삭제
        connectService.removeUserSession(sessionId);

        // 아이돌 접속 종료 처리
        Integer userId = (Integer) accessor.getSessionAttributes().get("userId");
        String role = (String) accessor.getSessionAttributes().get("role");

        if (userId != null && "IDOL".equals(role)) {
            chatService.setIdolOnline((long) userId, false);
            log.info("아이돌 접속 OFF: idolId={}", userId);
        }
        
        log.info("웹소켓 연결 종료: sessionId={}", sessionId);
    }
}
