package com.bit.idol.chatservice.service;

import com.bit.idol.chatservice.client.SubscriptionFeignClient;
import com.bit.idol.chatservice.client.UserFeignClient;
import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.bit.idol.chatservice.dto.ChatRoomListDto;
import com.bit.idol.chatservice.dto.IdolDto;
import com.bit.idol.chatservice.dto.SubscriptionDto;
import com.bit.idol.chatservice.dto.notification.NotificationEventDto;
import com.bit.idol.chatservice.dto.notification.TargetType;
import com.bit.idol.chatservice.entity.ChatMessage;
import com.bit.idol.chatservice.filter.ChatFilter;
import com.bit.idol.chatservice.filter.SuspiciousWordFilter;
import com.bit.idol.chatservice.producer.ChatProducer;
import com.bit.idol.chatservice.producer.NotificationProducer;
import com.bit.idol.chatservice.repository.ChatRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatFilter chatFilter;
    private final SuspiciousWordFilter suspiciousWordFilter;
    private final ChatProducer chatProducer;
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationProducer notificationProducer;
    private final UserFeignClient userFeignClient;
    private final SubscriptionFeignClient subscriptionFeignClient;
    private final ObjectMapper objectMapper;

    private static final long RATE_LIMIT_SECONDS = 3;
    private static final int CACHE_SIZE = 50;
    private static final String IDOL_CACHE_KEY = "idols:cache";

    // 채팅방 목록 조회 (Aggregation) - Redis 조회로 변경 (CQRS)
    @CircuitBreaker(name = "chat-room-list", fallbackMethod = "getChatRoomListFallback")
    public List<ChatRoomListDto> getChatRoomList(int userId) {
        // 1. 전체 아이돌 목록 조회 (Redis에서 조회)
        List<Object> cachedIdols = redisTemplate.opsForHash().values(IDOL_CACHE_KEY);
        List<IdolDto> idols;

        if (cachedIdols.isEmpty()) {
            // Redis에 없으면 Feign으로 조회하고 캐싱 (Cold Start 대비)
            log.warn("Redis에 아이돌 정보 없음. User Service 호출");
            idols = userFeignClient.getAllIdols();
            for (IdolDto idol : idols) {
                redisTemplate.opsForHash().put(IDOL_CACHE_KEY, String.valueOf(idol.getIdolId()), idol);
            }
        } else {
            idols = cachedIdols.stream()
                    .map(obj -> objectMapper.convertValue(obj, IdolDto.class))
                    .collect(Collectors.toList());
        }

        return buildChatRoomList(userId, idols);
    }

    // 그룹별 채팅방 목록 조회 (캐싱 적용)
    @CircuitBreaker(name = "chat-room-list", fallbackMethod = "getChatRoomListFallback")
    public List<ChatRoomListDto> getChatRoomListByGroup(int userId, int groupId) {
        String cacheKey = "groups:members:" + groupId;
        List<IdolDto> groupMembers;

        // 1. Redis 캐시 조회
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);

        if (cachedData != null) {
            groupMembers = objectMapper.convertValue(cachedData, new TypeReference<List<IdolDto>>() {
            });
        } else {
            // 2. 없으면 Feign 호출 및 캐싱 (TTL 1시간)
            groupMembers = userFeignClient.getIdolsByGroup(groupId);
            redisTemplate.opsForValue().set(cacheKey, groupMembers, Duration.ofHours(1));
        }

        // 3. 실시간 데이터 조합
        return buildChatRoomList(userId, groupMembers);
    }

    // 구독 여부 검증 (REST API 보안용)
    public void validateSubscription(int userId, Long idolId, String role) {
        if ("IDOL".equals(role) || "ADMIN".equals(role) || "AGENCY".equals(role)) {
            return; // 아이돌, 관리자, 소속사는 패스
        }

        try {
            List<SubscriptionDto> mySubscriptions = subscriptionFeignClient.getMySubscriptions(userId);
            boolean isSubscribed = mySubscriptions.stream()
                    .anyMatch(sub -> sub.getIdolId() == idolId.intValue());

            if (!isSubscribed) {
                throw new RuntimeException("구독하지 않은 채팅방입니다.");
            }
        } catch (Exception e) {
            log.error("구독 검증 실패 (userId={}, idolId={}): {}", userId, idolId, e.getMessage());
            throw new RuntimeException("구독 정보를 확인할 수 없거나 구독하지 않았습니다.");
        }
    }

    // 공통 로직 분리
    private List<ChatRoomListDto> buildChatRoomList(int userId, List<IdolDto> idols) {
        // 내 구독 목록 조회 (Subscription Service)
        Set<Integer> subscribedIdolIds = new HashSet<>();
        try {
            List<SubscriptionDto> mySubscriptions = subscriptionFeignClient.getMySubscriptions(userId);
            subscribedIdolIds = mySubscriptions.stream()
                    .map(SubscriptionDto::getIdolId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("구독 정보 조회 실패 (Fallback: 구독 정보 없이 목록 표시): {}", e.getMessage());
        }

        Set<Integer> finalSubscribedIdolIds = subscribedIdolIds;
        return idols.stream().map(idol -> {
            Long idolId = (long) idol.getIdolId();

            // 마지막 메시지 조회 (Redis 캐시 활용)
            Map<String, Object> preview = getChatPreview(idolId);
            String lastMessage = "";
            LocalDateTime lastMessageTime = null;

            if (preview != null) {
                lastMessage = (String) preview.get("content");
                String timeStr = (String) preview.get("time");
                String typeStr = (String) preview.get("type");

                if (typeStr != null) {
                    if ("IMAGE".equals(typeStr)) {
                        lastMessage = "사진을 보냈습니다.";
                    } else if ("VIDEO".equals(typeStr)) {
                        lastMessage = "동영상을 보냈습니다.";
                    }
                }

                if (timeStr != null) {
                    lastMessageTime = LocalDateTime.parse(timeStr);
                }
            }

            // 안 읽은 메시지 수 계산
            int unreadCount = 0;
            if (finalSubscribedIdolIds.contains(idol.getIdolId())) {
                String totalCountKey = "chat:room:" + idolId + ":total_count";
                String readCountKey = "chat:room:" + idolId + ":user:" + userId + ":last_read_count";

                Object totalObj = redisTemplate.opsForValue().get(totalCountKey);
                Object readObj = redisTemplate.opsForValue().get(readCountKey);

                int total;
                if (totalObj != null) {
                    total = Integer.parseInt(totalObj.toString());
                } else {
                    // Redis 캐시 만료 시 DB에서 아이돌이 보낸 총 메시지만카운트하여 복구
                    total = (int) chatRepository.countByIdolIdAndSenderRole(idolId, "IDOL");
                    redisTemplate.opsForValue().set(totalCountKey, String.valueOf(total), Duration.ofDays(30));
                }

                int read;
                if (readObj != null) {
                    read = Integer.parseInt(readObj.toString());
                } else {
                    // 신규 구독자이거나 읽음 캐시가 만료된 경우 (과거 메시지 안읽음 폭탄 방지)
                    read = total;
                    redisTemplate.opsForValue().set(readCountKey, String.valueOf(read), Duration.ofDays(30));
                }

                unreadCount = Math.max(0, total - read);
            }

            return ChatRoomListDto.builder()
                    .idolId(idolId)
                    .idolName(idol.getStageName())
                    .thumbnailUrl(idol.getProfileImage())
                    .lastMessage(lastMessage)
                    .lastMessageTime(lastMessageTime)
                    .unreadCount(unreadCount)
                    .subscribed(finalSubscribedIdolIds.contains(idol.getIdolId()))
                    .online(isIdolOnline(idolId))
                    .build();
        }).collect(Collectors.toList());
    }

    // Fallback: User Service 장애 시 빈 목록 반환
    public List<ChatRoomListDto> getChatRoomListFallback(int userId, Throwable t) {
        log.error("채팅방 목록 조회 실패 (User Service 장애): {}", t.getMessage());
        return Collections.emptyList();
    }

    // 오버로딩된 Fallback (파라미터 다름)
    public List<ChatRoomListDto> getChatRoomListFallback(int userId, int groupId, Throwable t) {
        log.error("그룹 채팅방 목록 조회 실패 (User Service 장애): {}", t.getMessage());
        return Collections.emptyList();
    }

    // 읽음 처리 (API 호출 시) - TTL 적용
    public void markAsRead(int userId, Long idolId) {
        String totalCountKey = "chat:room:" + idolId + ":total_count";
        String readCountKey = "chat:room:" + idolId + ":user:" + userId + ":last_read_count";

        Object totalObj = redisTemplate.opsForValue().get(totalCountKey);
        int total = totalObj != null ? Integer.parseInt(totalObj.toString()) : 0;

        // 30일 TTL 적용하여 현재 totalCount 값을 last_read_count에 동기화
        redisTemplate.opsForValue().set(readCountKey, String.valueOf(total), Duration.ofDays(30));
    }

    public void processMessage(ChatMessageDto messageDto) {
        // 0. 도배 방지
        if ("USER".equals(messageDto.getSenderRole())) {
            String limitKey = "chat:limit:" + messageDto.getSenderId();
            if (redisTemplate.hasKey(limitKey)) {
                throw new RuntimeException("메시지를 너무 빠르게 보낼 수 없습니다. 잠시 후 다시 시도해주세요.");
            }
            redisTemplate.opsForValue().set(limitKey, "1", Duration.ofSeconds(RATE_LIMIT_SECONDS));
        }

        try {
            // 1. 메시지 검열
            String filteredContent = chatFilter.filter(messageDto.getContent());
            messageDto.setContent(filteredContent);
        } catch (RuntimeException e) {
            log.warn("욕설 감지됨. 유저 신고 처리 (Kafka): userId={}, msg={}", messageDto.getSenderId(), e.getMessage());
            chatProducer.sendReport(messageDto.getSenderId());
            throw e;
        }

        // 2. MongoDB 저장 (PENDING 상태)
        ChatMessage chatMessage = ChatMessage.builder()
                .idolId(messageDto.getIdolId())
                .senderId(messageDto.getSenderId())
                .senderNickname(messageDto.getSenderNickname())
                .senderRole(messageDto.getSenderRole())
                .content(messageDto.getContent())
                .type(messageDto.getType())
                .thumbnailUrl(messageDto.getThumbnailUrl()) // 썸네일 URL 매핑 추가
                .parentId(messageDto.getParentId())
                .createdAt(LocalDateTime.now())
                .status("PENDING") // Outbox Pattern
                .build();

        ChatMessage savedMessage = chatRepository.save(chatMessage);

        messageDto.setId(savedMessage.getId());
        messageDto.setCreatedAt(savedMessage.getCreatedAt()); // Redis 캐싱 전 생성 시간 세팅

        // 3. Redis 캐싱 (TTL 적용)
        String cacheKey = "chat:room:" + messageDto.getIdolId();
        redisTemplate.opsForList().leftPush(cacheKey, messageDto);
        redisTemplate.opsForList().trim(cacheKey, 0, CACHE_SIZE - 1);
        redisTemplate.expire(cacheKey, Duration.ofDays(3)); // 3일 TTL

        // 3-1. Redis 미리보기 캐싱 (TTL 적용)
        String previewKey = "chat:preview:" + messageDto.getIdolId();
        Map<String, Object> previewData = new HashMap<>();
        previewData.put("content", messageDto.getContent());
        previewData.put("sender", messageDto.getSenderNickname());
        previewData.put("time", LocalDateTime.now().toString());
        previewData.put("type", messageDto.getType());
        redisTemplate.opsForValue().set(previewKey, previewData, Duration.ofDays(7)); // 7일 TTL

        // --- 3-2. 총 메시지 수 증가 (읽음 처리용) ---
        // 아이돌이 발송한 공지성 메시지만 글로벌 카운트를 증가시킵니다. (버블 스타일: 팬들의 메시지는 무시)
        if ("IDOL".equals(messageDto.getSenderRole())) {
            String totalCountKey = "chat:room:" + messageDto.getIdolId() + ":total_count";
            redisTemplate.opsForValue().increment(totalCountKey);
            redisTemplate.expire(totalCountKey, Duration.ofDays(30)); // 30일 TTL
        }

        // 내가 보낸 메시지는 바로 읽음 처리 (안 읽은 메시지 버그 방지)
        if ("USER".equals(messageDto.getSenderRole())) {
            markAsRead(messageDto.getSenderId(), messageDto.getIdolId());
        }
        // ----------------------------------------

        // 4. Kafka로 전송 (성공 시 SENT 업데이트)
        try {
            chatProducer.sendChatMessage(messageDto);

            // 전송 성공 -> 상태 업데이트
            savedMessage.setStatus("SENT");
            chatRepository.save(savedMessage);

            log.info("메시지 처리 완료: room={}, id={}", messageDto.getIdolId(), savedMessage.getId());
        } catch (Exception e) {
            log.error("Kafka 전송 실패 (재전송 대기): {}", e.getMessage());
            // 예외를 던지지 않고 PENDING 상태로 둠 -> 스케줄러가 처리
        }

        // 5. 알림 발송
        sendNotification(messageDto);

        // 6. AI 비동기 검사
        if (suspiciousWordFilter.isSuspicious(messageDto.getContent())) {
            log.info("의심 메시지 감지 -> AI 검사 요청: msgId={}", messageDto.getId());
            chatProducer.sendAiCheck(messageDto);
        }
    }

    // 미리보기 조회
    public Map<String, Object> getChatPreview(Long idolId) {
        String previewKey = "chat:preview:" + idolId;
        Object data = redisTemplate.opsForValue().get(previewKey);

        if (data != null) {
            return (Map<String, Object>) data;
        }

        Pageable pageable = PageRequest.of(0, 1);
        List<ChatMessage> messages = chatRepository.findByIdolIdOrderByIdDesc(idolId, pageable);

        if (!messages.isEmpty()) {
            ChatMessage lastMsg = messages.get(0);
            Map<String, Object> preview = new HashMap<>();
            preview.put("content", lastMsg.getContent());
            preview.put("sender", lastMsg.getSenderNickname());
            preview.put("time", lastMsg.getCreatedAt().toString());
            preview.put("type", lastMsg.getType());

            redisTemplate.opsForValue().set(previewKey, preview, Duration.ofDays(7)); // 조회 시에도 TTL 갱신
            return preview;
        }

        return null;
    }

    // --- 공지사항 (Pinned Message) ---

    public void pinMessage(String messageId, Long idolId) {
        ChatMessage message = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("메시지를 찾을 수 없습니다."));

        String pinKey = "chat:pin:" + idolId;
        ChatMessageDto pinDto = convertToDto(message);
        redisTemplate.opsForValue().set(pinKey, pinDto); // 공지는 영구 저장 (TTL 없음)

        pinDto.setType("PIN");
        chatProducer.sendChatMessage(pinDto);

        log.info("공지사항 등록 완료: room={}, msgId={}", idolId, messageId);
    }

    public void unpinMessage(Long idolId) {
        String pinKey = "chat:pin:" + idolId;
        redisTemplate.delete(pinKey);

        ChatMessageDto unpinEvent = ChatMessageDto.builder()
                .idolId(idolId)
                .type("UNPIN")
                .build();
        chatProducer.sendChatMessage(unpinEvent);

        log.info("공지사항 해제 완료: room={}", idolId);
    }

    public ChatMessageDto getPinnedMessage(Long idolId) {
        String pinKey = "chat:pin:" + idolId;
        Object data = redisTemplate.opsForValue().get(pinKey);

        if (data != null) {
            return objectMapper.convertValue(data, ChatMessageDto.class);
        }
        return null;
    }

    // --- 미디어 모아보기 ---

    public List<ChatMessageDto> getChatMedia(int userId, String role, Long idolId, String lastId, int size) {
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "id"));
        List<ChatMessage> messages;

        boolean isIdolOrAdmin = "IDOL".equals(role) || "ADMIN".equals(role) || "AGENCY".equals(role);

        if (isIdolOrAdmin) {
            if (lastId == null) {
                messages = chatRepository.findMediaByIdolId(idolId, pageable);
            } else {
                messages = chatRepository.findMediaByIdolIdAndIdLessThan(idolId, lastId, pageable);
            }
        } else {
            // 그룹/아이돌 멤버별 커스텀 뷰 - 내가 보낸 미디어 + 아이돌 미디어만
            if (lastId == null) {
                messages = chatRepository.findUserMediaByIdolId(idolId, userId, pageable);
            } else {
                messages = chatRepository.findUserMediaByIdolIdAndIdLessThan(idolId, userId, lastId, pageable);
            }
        }

        return messages.stream()
                .map(this::convertToDto)
                .peek(dto -> dto.setMe(dto.getSenderId() == userId)) // isMe 설정
                .collect(Collectors.toList());
    }

    // --------------------------------

    private void sendNotification(ChatMessageDto messageDto) {
        try {
            if ("IDOL".equals(messageDto.getSenderRole())) {
                Map<String, String> args = new HashMap<>();
                args.put("idolName", messageDto.getSenderNickname());
                args.put("message", messageDto.getContent());

                NotificationEventDto event = NotificationEventDto.builder()
                        .eventId(UUID.randomUUID().toString())
                        .type("IDOL_MESSAGE")
                        .targetType(TargetType.IDOL_SUB)
                        .targetId(String.valueOf(messageDto.getIdolId()))
                        .args(args)
                        .redirectUrl("/chat/room/" + messageDto.getIdolId())
                        .occurredAt(LocalDateTime.now())
                        .build();

                notificationProducer.send(event);
            }

            if (messageDto.getParentId() != null && !messageDto.getParentId().isEmpty()) {
                chatRepository.findById(messageDto.getParentId()).ifPresent(parentMsg -> {
                    if (parentMsg.getSenderId() != messageDto.getSenderId()) {
                        Map<String, String> args = new HashMap<>();
                        args.put("replierName", messageDto.getSenderNickname());

                        NotificationEventDto event = NotificationEventDto.builder()
                                .eventId(UUID.randomUUID().toString())
                                .type("REPLY_MESSAGE")
                                .targetType(TargetType.USER)
                                .targetId(String.valueOf(parentMsg.getSenderId()))
                                .args(args)
                                .redirectUrl("/chat/room/" + messageDto.getIdolId())
                                .occurredAt(LocalDateTime.now())
                                .build();

                        notificationProducer.send(event);
                    }
                });
            }
        } catch (Exception e) {
            log.error("알림 발송 중 오류: {}", e.getMessage());
        }
    }

    public void addReaction(String messageId, String reactionType, Long idolId) {
        ChatMessage message = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("메시지를 찾을 수 없습니다."));

        Map<String, Integer> reactions = message.getReactions();
        if (reactions == null) {
            reactions = new HashMap<>();
        }
        reactions.put(reactionType, reactions.getOrDefault(reactionType, 0) + 1);

        ChatMessage updatedMessage = ChatMessage.builder()
                .id(message.getId())
                .idolId(message.getIdolId())
                .senderId(message.getSenderId())
                .senderNickname(message.getSenderNickname())
                .senderRole(message.getSenderRole())
                .content(message.getContent())
                .type(message.getType())
                .thumbnailUrl(message.getThumbnailUrl()) // 썸네일 매핑
                .parentId(message.getParentId())
                .reactions(reactions)
                .createdAt(message.getCreatedAt())
                .build();

        chatRepository.save(updatedMessage);

        ChatMessageDto reactionEvent = ChatMessageDto.builder()
                .id(messageId)
                .idolId(idolId)
                .type("REACTION")
                .reactions(reactions)
                .build();

        chatProducer.sendChatMessage(reactionEvent);

        log.info("반응 추가 완료: msgId={}, type={}", messageId, reactionType);
    }

    public void deleteMessage(String messageId, Long idolId, int userId) {
        ChatMessage message = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("메시지를 찾을 수 없습니다."));

        if (message.getSenderId() != userId) {
            throw new RuntimeException("본인의 메시지만 삭제할 수 있습니다.");
        }

        ChatMessage deletedMessage = ChatMessage.builder()
                .id(message.getId())
                .idolId(message.getIdolId())
                .senderId(message.getSenderId())
                .senderNickname(message.getSenderNickname())
                .senderRole(message.getSenderRole())
                .content("삭제된 메시지입니다.")
                .type("DELETED")
                .parentId(message.getParentId())
                .reactions(message.getReactions())
                .createdAt(message.getCreatedAt())
                .build();

        chatRepository.save(deletedMessage);

        ChatMessageDto deleteEvent = ChatMessageDto.builder()
                .id(messageId)
                .idolId(idolId)
                .type("DELETE")
                .build();

        chatProducer.sendChatMessage(deleteEvent);

        log.info("메시지 삭제 처리 완료: id={}", messageId);
    }

    public boolean isIdolOnline(Long idolId) {
        String onlineKey = "idol:online:sessions:" + idolId;
        Long size = redisTemplate.opsForSet().size(onlineKey);
        return size != null && size > 0;
    }

    // 다중 탭/기기 환경을 고려한 접속 상태 집합 관리
    public void setIdolOnline(Long idolId, boolean isOnline, String sessionId) {
        String onlineKey = "idol:online:sessions:" + idolId;

        Long previousCount = redisTemplate.opsForSet().size(onlineKey);
        if (previousCount == null)
            previousCount = 0L;

        if (isOnline) {
            redisTemplate.opsForSet().add(onlineKey, sessionId);
        } else {
            redisTemplate.opsForSet().remove(onlineKey, sessionId);
        }

        Long currentCount = redisTemplate.opsForSet().size(onlineKey);
        if (currentCount == null)
            currentCount = 0L;

        // 브로드캐스팅 최적화: 0 -> 1 (최초 접속) 이거나, 1 -> 0 (최종 종료) 일 때만 전파
        boolean turnedOn = (previousCount == 0 && currentCount > 0);
        boolean turnedOff = (previousCount > 0 && currentCount == 0);

        if (turnedOn || turnedOff) {
            // 상태 변경 실시간 브로드캐스팅 (프론트엔드 실시간 반영용)
            ChatMessageDto statusMessage = ChatMessageDto.builder()
                    .idolId(idolId)
                    .type("STATUS")
                    .content(turnedOn ? "ON" : "OFF")
                    .build();

            redisTemplate.convertAndSend("/sub/idol/" + idolId, statusMessage);
            log.info("아이돌 접속 상태 브로드캐스트 전송: idolId={}, event={}", idolId,
                    turnedOn ? "User Joined" : "All Users Disconnected");

            // 아이돌 최초 접속 시 구독자 알림 발행
            if (turnedOn) {
                try {
                    String idolName = userFeignClient.getAllIdols().stream()
                            .filter(idol -> idol.getIdolId() == idolId)
                            .findFirst()
                            .map(idol -> idol.getStageName())
                            .orElse("아이돌");
                    com.bit.idol.chatservice.dto.notification.NotificationEventDto notifyEvent =
                            com.bit.idol.chatservice.dto.notification.NotificationEventDto.builder()
                                    .eventId(java.util.UUID.randomUUID().toString())
                                    .type("CHAT_IDOL_ONLINE")
                                    .targetType(com.bit.idol.chatservice.dto.notification.TargetType.IDOL_SUB)
                                    .targetId(String.valueOf(idolId))
                                    .args(java.util.Map.of("idolName", idolName))
                                    .redirectUrl("/chat/room/" + idolId)
                                    .occurredAt(java.time.LocalDateTime.now())
                                    .build();
                    notificationProducer.send(notifyEvent);
                } catch (Exception e) {
                    log.error("아이돌 접속 알림 발행 실패: idolId={}, err={}", idolId, e.getMessage());
                }
            }
        }
    }

    public List<ChatMessageDto> getChatHistory(int userId, String role, Long idolId, String lastId, int size) {
        List<ChatMessageDto> result = new ArrayList<>();

        boolean isIdolOrAdmin = "IDOL".equals(role) || "ADMIN".equals(role) || "AGENCY".equals(role);

        // 1. Redis 캐시 조회 (최신 메시지인 경우만, 스태프/아이돌만 캐시 활용)
        if (lastId == null && isIdolOrAdmin) {
            String cacheKey = "chat:room:" + idolId;
            List<Object> cachedMessages = redisTemplate.opsForList().range(cacheKey, 0, size - 1);

            if (cachedMessages != null && !cachedMessages.isEmpty()) {
                List<ChatMessageDto> redisDtos = cachedMessages.stream()
                        .map(obj -> objectMapper.convertValue(obj, ChatMessageDto.class))
                        .collect(Collectors.toList());
                result.addAll(redisDtos);
            }
        }

        // 2. 부족한 만큼 DB에서 추가 조회
        if (result.size() < size) {
            int needMore = size - result.size();
            Pageable pageable = PageRequest.of(0, needMore);
            List<ChatMessage> dbMessages;

            String queryLastId = lastId;
            // Redis에서 일부를 가져왔다면 그 다음부터 페이징
            if (lastId == null && isIdolOrAdmin && !result.isEmpty()) {
                queryLastId = result.get(result.size() - 1).getId();
            }

            if (isIdolOrAdmin) {
                if (queryLastId == null) {
                    dbMessages = chatRepository.findByIdolIdOrderByIdDesc(idolId, pageable);
                } else {
                    dbMessages = chatRepository.findByIdolIdAndIdLessThanOrderByIdDesc(idolId, queryLastId, pageable);
                }
            } else {
                // 팬(USER)의 경우: 내가 보낸 채팅 + 아이돌의 메시지만 필터링 (Bubble 1:N 구조)
                if (queryLastId == null) {
                    dbMessages = chatRepository.findUserMessagesByIdolIdOrderByIdDesc(idolId, userId, pageable);
                } else {
                    dbMessages = chatRepository.findUserMessagesByIdolIdAndIdLessThanOrderByIdDesc(idolId, userId,
                            queryLastId, pageable);
                }
            }

            List<ChatMessageDto> dbDtos = dbMessages.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            result.addAll(dbDtos);
        }

        // 3. isMe 필드 설정 및 중복 제거 (프론트엔드 React key 에러 방지용 최후 방어선)
        Set<String> seenIds = new HashSet<>();
        List<ChatMessageDto> deduplicatedResult = new ArrayList<>();

        for (ChatMessageDto dto : result) {
            if (dto.getId() != null && seenIds.contains(dto.getId())) {
                continue; // 이미 추가된 메시지면 무시
            }
            if (dto.getId() != null) {
                seenIds.add(dto.getId());
            }
            dto.setMe(dto.getSenderId() == userId);
            deduplicatedResult.add(dto);
        }

        return deduplicatedResult;
    }

    private ChatMessageDto convertToDto(ChatMessage entity) {
        return ChatMessageDto.builder()
                .id(entity.getId())
                .idolId(entity.getIdolId())
                .senderId(entity.getSenderId())
                .senderNickname(entity.getSenderNickname())
                .senderRole(entity.getSenderRole())
                .content(entity.getContent())
                .type(entity.getType())
                .parentId(entity.getParentId())
                .reactions(entity.getReactions())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
