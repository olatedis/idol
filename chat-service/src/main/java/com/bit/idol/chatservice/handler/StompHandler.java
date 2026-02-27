package com.bit.idol.chatservice.handler;

import com.bit.idol.chatservice.client.UserFeignClient;
import com.bit.idol.chatservice.dto.IdolDto;
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

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final ConnectService connectService;
    private final ChatService chatService;
    private final UserFeignClient userFeignClient;

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
                    case SUBSCRIBE:
                        handleSubscribe(accessor);
                        break;
                    case DISCONNECT:
                        handleDisconnect(accessor);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                log.error("STOMP 처리 중 오류 발생: command={}, error={}", accessor.getCommand(), e.getMessage());
                throw e; // 예외를 다시 던져서 클라이언트에게 알림
            }
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        log.info("CONNECT 요청 수신: sessionId={}", accessor.getSessionId());

        // 1. 토큰 검증
        String token = accessor.getFirstNativeHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("인증 토큰이 없습니다.");
        }

        UserDto user = connectService.verifyUser(token);
        log.info("유저 검증 성공: userId={}, role={}", user.getUserId(), user.getRole());

        // 2. 구독 목록 조회 및 세션 저장 (보안 강화)
        if ("USER".equals(user.getRole())) {
            Set<Long> subscribedIdolIds = connectService.getSubscribedIdolIds(user.getUserId());
            accessor.getSessionAttributes().put("subscribedIdolIds", subscribedIdolIds);
            log.info("구독 목록 로드 완료: {}개", subscribedIdolIds.size());
        } else if ("IDOL".equals(user.getRole())) {
            try {
                IdolDto idol = userFeignClient.getMyIdolInfo(user.getUserId());
                if (idol != null) {
                    String currentSessionId = accessor.getSessionId();
                    chatService.setIdolOnline((long) idol.getIdolId(), true,
                            currentSessionId != null ? currentSessionId : "");
                    accessor.getSessionAttributes().put("idolId", idol.getIdolId());
                    log.info("아이돌 접속 ON: idolId={}, sessionId={}", idol.getIdolId(), currentSessionId);
                }
            } catch (Exception e) {
                log.error("아이돌 정보 조회 실패 (접속 상태 미반영): userId={}, error={}", user.getUserId(), e.getMessage());
            }
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
        // 1. 세션 확인
        Integer userId = (Integer) accessor.getSessionAttributes().get("userId");
        String role = (String) accessor.getSessionAttributes().get("role");

        if (userId == null) {
            throw new RuntimeException("세션이 만료되었습니다.");
        }

        // 2. 목적지(Destination) 파싱 -> idolId 추출
        String destination = accessor.getDestination(); // 예: /pub/chat/send (메시지 본문에 idolId 있음)

        // 메시지 본문(Payload)을 여기서 까보긴 어렵습니다. (MessageConverter 전이라 byte[] 상태임)
        // 대신, 클라이언트가 헤더에 'idolId'를 보내도록 강제하거나,
        // destination을 '/pub/chat/{idolId}/send' 형태로 바꾸는 것이 좋습니다.

        // 현재 구조상 Payload 검증이 어려우므로,
        // 1차적으로 세션에 구독 목록이 있는지만 확인합니다.
        // (더 강력한 보안을 위해선 Controller에서 @DestinationVariable로 받아서 검증해야 함)

        if ("USER".equals(role)) {
            @SuppressWarnings("unchecked")
            Set<Long> subscribedIdolIds = (Set<Long>) accessor.getSessionAttributes().get("subscribedIdolIds");

            if (subscribedIdolIds == null || subscribedIdolIds.isEmpty()) {
                // 구독 정보가 없으면 전송 차단 (혹은 재조회 시도)
                throw new RuntimeException("구독 정보가 없습니다.");
            }

            // 상세 검증은 Controller의 @MessageMapping에서 수행하는 것이 더 자연스러움.
            // 여기서는 "구독자만 채팅 가능"이라는 대전제만 체크.
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String role = (String) accessor.getSessionAttributes().get("role");
        String destination = accessor.getDestination(); // 예: /sub/idol/{idolId}

        if ("USER".equals(role) && destination != null) {
            // 정규식을 사용하거나 단순히 파싱하여 idolId 추출
            // 예: /sub/idol/123 -> idolId = 123
            try {
                if (destination.startsWith("/sub/idol/")) {
                    String[] parts = destination.split("/");
                    if (parts.length >= 4) {
                        Long targetIdolId = Long.parseLong(parts[3]);

                        @SuppressWarnings("unchecked")
                        Set<Long> subscribedIdolIds = (Set<Long>) accessor.getSessionAttributes()
                                .get("subscribedIdolIds");

                        if (subscribedIdolIds == null || !subscribedIdolIds.contains(targetIdolId)) {
                            log.warn("권한 없는 채팅방 구독 시도 차단: sessionId={}, targetIdolId={}", accessor.getSessionId(),
                                    targetIdolId);
                            throw new RuntimeException("구독하지 않은 채팅방은 실시간 수신할 수 없습니다.");
                        }
                    }
                }
            } catch (Exception e) {
                log.error("구독 채널 검증 중 오류: {}", e.getMessage());
                throw new RuntimeException("구독 채널 검증 실패");
            }
        }
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        connectService.removeUserSession(sessionId);

        Integer userId = (Integer) accessor.getSessionAttributes().get("userId");
        String role = (String) accessor.getSessionAttributes().get("role");
        Integer idolId = (Integer) accessor.getSessionAttributes().get("idolId");

        if (userId != null && "IDOL".equals(role) && idolId != null) {
            chatService.setIdolOnline((long) idolId, false, sessionId);
            log.info("아이돌 접속 OFF: idolId={}, sessionId={}", idolId, sessionId);
        }

        log.info("웹소켓 연결 종료: sessionId={}", sessionId);
    }
}
