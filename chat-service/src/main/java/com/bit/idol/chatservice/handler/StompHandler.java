package com.bit.idol.chatservice.handler;

import com.bit.idol.chatservice.client.SubscriptionFeignClient;
import com.bit.idol.chatservice.client.UserFeignClient;
import com.bit.idol.chatservice.dto.UserDto;
import com.bit.idol.chatservice.service.ChatService;
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
    private final ChatService chatService; // 접속 상태 관리를 위해 추가

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            // 1. 연결 요청 (CONNECT)
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                handleConnect(accessor);
            }
            // 2. 연결 종료 (DISCONNECT)
            else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                handleDisconnect(accessor);
            }
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
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
        // 3. 아이돌인 경우 접속 상태 ON (Redis)
        else if ("IDOL".equals(user.getRole())) {
            // 아이돌 ID는 토큰에서 추출한 userId와 동일하다고 가정 (또는 별도 매핑 필요)
            // 여기서는 userId를 idolId로 사용
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
        // 세션에서 정보 꺼내기
        Integer userId = (Integer) accessor.getSessionAttributes().get("userId");
        String role = (String) accessor.getSessionAttributes().get("role");

        if (userId != null && "IDOL".equals(role)) {
            // 아이돌 접속 상태 OFF (Redis)
            chatService.setIdolOnline((long) userId, false);
            log.info("아이돌 접속 OFF: idolId={}", userId);
        }
    }
}
