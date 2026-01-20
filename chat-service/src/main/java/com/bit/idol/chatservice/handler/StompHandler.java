package com.bit.idol.chatservice.handler;

import com.bit.idol.chatservice.client.SubscriptionFeignClient;
import com.bit.idol.chatservice.client.UserFeignClient;
import com.bit.idol.chatservice.dto.UserDto;
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

    private final UserFeignClient userFeignClient;
    private final SubscriptionFeignClient subscriptionFeignClient;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 1. 토큰 검증
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                throw new RuntimeException("인증 토큰이 없습니다.");
            }

            UserDto user;
            try {
                user = userFeignClient.getUserInfo(token);
            } catch (Exception e) {
                log.error("토큰 검증 실패: {}", e.getMessage());
                throw new RuntimeException("유효하지 않은 토큰입니다.");
            }

            // 2. 구독 여부 확인 (USER인 경우만)
            if ("USER".equals(user.getRole())) {
                String idolIdStr = accessor.getFirstNativeHeader("idolId");
                if (idolIdStr == null) {
                    throw new RuntimeException("idolId 헤더가 필요합니다.");
                }

                try {
                    Long idolId = Long.parseLong(idolIdStr);
                    boolean isSubscribed = subscriptionFeignClient.checkSubscription(user.getUserId(), idolId);
                    
                    if (!isSubscribed) {
                        throw new RuntimeException("구독하지 않은 아이돌입니다.");
                    }
                } catch (Exception e) {
                    log.error("구독 확인 실패: {}", e.getMessage());
                    throw new RuntimeException("구독 확인 중 오류가 발생했습니다.");
                }
            }

            // 3. 세션에 유저 정보 저장 (나중에 메시지 보낼 때 사용)
            accessor.getSessionAttributes().put("userId", user.getUserId());
            accessor.getSessionAttributes().put("role", user.getRole());
            accessor.getSessionAttributes().put("nickname", user.getNickname());
            
            log.info("웹소켓 연결 성공: userId={}, role={}", user.getUserId(), user.getRole());
        }

        return message;
    }
}
