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

    private final ConnectService connectService; // 서킷 브레이커 적용된 서비스
    private final ChatService chatService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                handleConnect(accessor);
            } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                handleDisconnect(accessor);
            }
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        // 1. 토큰 검증 (서킷 브레이커 적용)
        String token = accessor.getFirstNativeHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("인증 토큰이 없습니다.");
        }

        UserDto user = connectService.verifyUser(token);

        // 2. 구독 여부 확인 (USER인 경우만, 서킷 브레이커 적용)
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

        // 4. 세션에 유저 정보 저장
        accessor.getSessionAttributes().put("userId", user.getUserId());
        accessor.getSessionAttributes().put("role", user.getRole());
        accessor.getSessionAttributes().put("nickname", user.getNickname());
        
        log.info("웹소켓 연결 성공: userId={}, role={}", user.getUserId(), user.getRole());
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        Integer userId = (Integer) accessor.getSessionAttributes().get("userId");
        String role = (String) accessor.getSessionAttributes().get("role");

        if (userId != null && "IDOL".equals(role)) {
            chatService.setIdolOnline((long) userId, false);
            log.info("아이돌 접속 OFF: idolId={}", userId);
        }
    }
}
