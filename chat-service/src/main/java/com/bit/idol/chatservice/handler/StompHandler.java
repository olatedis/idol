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
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final ConnectService connectService;
    private final ChatService chatService;
    private final UserFeignClient userFeignClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

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
                    case UNSUBSCRIBE:
                        handleUnsubscribe(accessor);
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

        // 2. 구독 목록은 세션에 저장하지 않고 필요 시 실시간 조회 (스태일 세션 방지)
        if ("IDOL".equals(user.getRole())) {
            try {
                IdolDto idol = userFeignClient.getMyIdolInfo(user.getUserId());
                if (idol != null) {
                    // 이제 단순히 연결됐다고 온라인으로 표시하지 않음 (방 입장 시 SUBSCRIBE 에서 처리)
                    accessor.getSessionAttributes().put("idolId", idol.getIdolId());
                    log.info("아이돌 세션 준비 완료: idolId={}, sessionId={}", idol.getIdolId(), accessor.getSessionId());
                }
            } catch (Exception e) {
                log.error("아이돌 정보 조회 실패: userId={}, error={}", user.getUserId(), e.getMessage());
            }
        }

        // 3. Redis 저장
        connectService.saveUserSession(accessor.getSessionId(), user);

        // 4. 세션 속성 저장
        accessor.getSessionAttributes().put("userId", user.getUserId());
        accessor.getSessionAttributes().put("role", user.getRole());
        accessor.getSessionAttributes().put("nickname", user.getNickname());

        // [추가] 프로필 이미지 정보 세션 보관 (JWT 대신 Feign 1회 호출로 최신화)
        try {
            UserDto fullUser = userFeignClient.getUserInfoById(user.getUserId());
            if (fullUser != null && fullUser.getImgUrl() != null) {
                accessor.getSessionAttributes().put("profileImage", fullUser.getImgUrl());
                log.info("유저 프로필 이미지 세션 저장 완료: userId={}, imgUrl={}", user.getUserId(), fullUser.getImgUrl());
            }
        } catch (Exception e) {
            log.warn("유저 프로필 이미지 조회 실패 (기본값 사용): {}", e.getMessage());
        }

        log.info("웹소켓 연결 최종 승인: sessionId={}", accessor.getSessionId());
    }

    private void handleSend(StompHeaderAccessor accessor) {
        // 1. 세션 확인
        Integer userId = (Integer) accessor.getSessionAttributes().get("userId");
        String role = (String) accessor.getSessionAttributes().get("role");

        if (userId == null) {
            throw new RuntimeException("세션이 만료되었습니다.");
        }

        // Redis 조회: 유저 상태가 RESTRICTED 등 제재 상태이면 발송 차단
        String userCacheKey = "user:info:id::" + userId;
        String userJson = stringRedisTemplate.opsForValue().get(userCacheKey);
        if (userJson != null) {
            try {
                JsonNode rootNode = objectMapper.readTree(userJson);
                JsonNode statusNode = rootNode.has("status") ? rootNode.get("status")
                        : (rootNode.isArray() && rootNode.size() > 1 && rootNode.get(1).has("status")
                                ? rootNode.get(1).get("status")
                                : null);

                if (statusNode != null) {
                    String status = statusNode.asText();
                    if ("RESTRICTED".equals(status) || "SUSPENDED".equals(status) || "BANNED".equals(status)) {
                        throw new RuntimeException("활동이 제한된 계정입니다.");
                    }
                }
            } catch (RuntimeException e) {
                if (e.getMessage().equals("활동이 제한된 계정입니다.")) {
                    throw e;
                }
                log.warn("STOMP 유저 제재 상태 확인 중 오류 (계속 진행): {}", e.getMessage());
            } catch (Exception e) {
                log.warn("STOMP 유저 제재 상태 파싱 중 오류 (계속 진행): {}", e.getMessage());
            }
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
            // 실시간 구독 여부 확인 (Redis 조회)
            // Payload를 파싱하여 idolId를 추출하는 대신, Controller에서 이미 검증하지만
            // Handler 레벨에서 1차 차단을 원할 경우 헤더에 idolId를 보내거나, 
            // 여기서는 '로그인 여부'만 체크하고 상세 검증은 Controller에 맡김.
            // (이미 handleConnect에서 세션이 없으면 에러가 남)
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String role = (String) accessor.getSessionAttributes().get("role");
        String destination = accessor.getDestination(); // 예: /sub/idol/{idolId}

        if (destination != null) {
            // 1. 유저의 구독 권한 검증
            if ("USER".equals(role)) {
                try {
                    if (destination.startsWith("/sub/idol/")) {
                        String[] parts = destination.split("/");
                        if (parts.length >= 4) {
                            Long targetIdolId = Long.parseLong(parts[3]);
                            int userIdInt = (int) accessor.getSessionAttributes().get("userId");
                            boolean isSubscribed = connectService.isSubscribed(userIdInt, targetIdolId.intValue());

                            if (!isSubscribed) {
                                log.warn("권한 없는 채팅방 구독 시도 차단: sessionId={}, userId={}, targetIdolId={}", 
                                        accessor.getSessionId(), userIdInt, targetIdolId);
                                throw new RuntimeException("구독하지 않은 채팅방은 실시간 수신할 수 없습니다.");
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("구독 채널 검증 중 오류: {}", e.getMessage());
                    throw new RuntimeException("구독 채널 검증 실패");
                }
            }

            // 2. [추가] 아이돌 본인의 방 입장 실시간 감지 (온라인 상태 ON)
            if ("IDOL".equals(role)) {
                Integer myIdolId = (Integer) accessor.getSessionAttributes().get("idolId");
                if (myIdolId != null && (destination.contains("/sub/idol/" + myIdolId) || destination.contains("/queue/idol/" + myIdolId))) {
                    chatService.setIdolOnline((long) myIdolId, true, accessor.getSessionId());
                }
            }
        }
    }

    private void handleUnsubscribe(StompHeaderAccessor accessor) {
        String role = (String) accessor.getSessionAttributes().get("role");
        Integer idolId = (Integer) accessor.getSessionAttributes().get("idolId");
        String sessionId = accessor.getSessionId();

        if ("IDOL".equals(role) && idolId != null) {
            // 언구독 시 온라인 상태를 체크하여 뺌 (방에서 나감)
            chatService.setIdolOnline((long) idolId, false, sessionId);
            log.info("아이돌 방 퇴장 (UNSUBSCRIBE): idolId={}, sessionId={}", idolId, sessionId);
        }
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        Integer userId = (Integer) accessor.getSessionAttributes().get("userId");
        String role = (String) accessor.getSessionAttributes().get("role");
        Integer idolId = (Integer) accessor.getSessionAttributes().get("idolId");

        // 1. 온라인 상태 먼저 체크 및 필요 시 브로드캐스트 (세션 레코드가 삭제되기 전에 수행해야 정확함)
        if (userId != null && "IDOL".equals(role) && idolId != null) {
            chatService.setIdolOnline((long) idolId, false, sessionId);
            log.info("아이돌 접속 OFF 시도: idolId={}, sessionId={}", idolId, sessionId);
        }

        // 2. 그 다음 실제 세션 정보 삭제
        connectService.removeUserSession(sessionId);
        log.info("웹소켓 세션 및 로그아웃 처리 완료: sessionId={}", sessionId);
    }
}
