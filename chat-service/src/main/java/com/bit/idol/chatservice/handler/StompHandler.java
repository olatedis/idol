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
            try {
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
            } catch (Exception e) {
                log.error("STOMP 처리 중 오류 발생: command={}, error={}", accessor.getCommand(), e.getMessage(), e);
                throw e; // 예외를 다시 던져서 클라이언트에게 알림
            }
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        log.info("CONNECT 요청 수신: sessionId={}", accessor.getSessionId());

        // 1. 토큰 검증
        String token = accessor.getFirstNativeHeader("Authorization");
        log.debug("Authorization Header: {}", token);
        
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("인증 토큰이 없습니다.");
        }

        UserDto user = connectService.verifyUser(token);
        log.info("유저 검증 성공: userId={}, role={}", user.getUserId(), user.getRole());

        // 2. 구독 여부 확인
        if ("USER".equals(user.getRole())) {
            String idolIdStr = accessor.getFirstNativeHeader("idolId");
            log.debug("idolId Header: {}", idolIdStr);
            
            if (idolIdStr == null) {
                throw new RuntimeException("idolId 헤더가 필요합니다.");
            }

            Long idolId = Long.parseLong(idolIdStr);
            boolean isSubscribed = connectService.verifySubscription(user.getUserId(), idolId);
            
            if (!isSubscribed) {
                throw new RuntimeException("구독하지 않은 아이돌입니다.");
            }
            log.info("구독 확인 성공: userId={}, idolId={}", user.getUserId(), idolId);
        }
        else if ("IDOL".equals(user.getRole())) {
            chatService.setIdolOnline((long) user.getUserId(), true);
            log.info("아이돌 접속 ON: idolId={}", user.getUserId());
        }

        // 3. Redis 저장
        connectService.saveUserSession(accessor.getSessionId(), user);
        
        // 4. 세션 속성 저장
        accessor.getSessionAttributes().put("userId", user.getUserId());
        accessor.getSessionAttributes().put("role", user.getRole());
        accessor.getSessionAttributes().put("nickname", user.getNickname());
        
        log.info("웹소켓 연결 최종 승인: sessionId={}", accessor.getSessionId());
    }

    private void handleSend(StompHeaderAccessor accessor) {
        UserDto user = connectService.getUserSession(accessor.getSessionId());
        
        if (user == null) {
            Integer userId = (Integer) accessor.getSessionAttributes().get("userId");
            if (userId == null) {
                throw new RuntimeException("세션이 만료되었습니다.");
            }
        }
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        connectService.removeUserSession(sessionId);

        Integer userId = (Integer) accessor.getSessionAttributes().get("userId");
        String role = (String) accessor.getSessionAttributes().get("role");

        if (userId != null && "IDOL".equals(role)) {
            chatService.setIdolOnline((long) userId, false);
            log.info("아이돌 접속 OFF: idolId={}", userId);
        }
        
        log.info("웹소켓 연결 종료: sessionId={}", sessionId);
    }
}
