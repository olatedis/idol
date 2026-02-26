package com.bit.idol.chatservice.controller;

import com.bit.idol.chatservice.client.UserFeignClient;
import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.bit.idol.chatservice.dto.ChatRoomListDto;
import com.bit.idol.chatservice.dto.FileUploadResponseDto;
import com.bit.idol.chatservice.dto.UserDto;
import com.bit.idol.chatservice.service.ChatService;
import com.bit.idol.chatservice.service.S3Service;
import com.bit.idol.chatservice.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final S3Service s3Service;
    private final TranslationService translationService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserFeignClient userFeignClient; // 추가됨

    // --- 벤치마크 API (추가됨) ---
    @GetMapping("/benchmark/feign")
    @ResponseBody
    public ResponseEntity<UserDto> benchmarkFeign() {
        // 1번 유저 정보 조회 (단순 호출 테스트)
        return ResponseEntity.ok(userFeignClient.getUserInfoById(1));
    }
    // ---------------------------

    @MessageMapping("/chat/send")
    public void sendMessage(ChatMessageDto messageDto, SimpMessageHeaderAccessor accessor) {
        Integer userId = (Integer) accessor.getSessionAttributes().get("userId");
        String role = (String) accessor.getSessionAttributes().get("role");
        String nickname = (String) accessor.getSessionAttributes().get("nickname");

        if (userId == null) {
            log.error("인증된 유저 정보가 없습니다.");
            return;
        }

        // --- 권한 검사 추가 (보안 강화) ---
        if ("USER".equals(role)) {
            @SuppressWarnings("unchecked")
            Set<Long> subscribedIdolIds = (Set<Long>) accessor.getSessionAttributes().get("subscribedIdolIds");

            if (subscribedIdolIds == null || !subscribedIdolIds.contains(messageDto.getIdolId())) {
                log.warn("권한 없는 채팅 시도 차단: userId={}, idolId={}", userId, messageDto.getIdolId());
                throw new RuntimeException("구독하지 않은 채팅방입니다.");
            }
        } else if ("IDOL".equals(role)) {
            Integer myIdolId = (Integer) accessor.getSessionAttributes().get("idolId");
            if (myIdolId == null || !myIdolId.equals(messageDto.getIdolId().intValue())) {
                log.warn("다른 아이돌 방에 채팅 시도 차단: myIdolId={}, targetIdolId={}", myIdolId, messageDto.getIdolId());
                throw new RuntimeException("자신의 채팅방에서만 메시지를 보낼 수 있습니다.");
            }
        }
        // --------------------------------

        messageDto.setSenderId(userId);
        messageDto.setSenderRole(role);
        messageDto.setSenderNickname(nickname);

        log.info("메시지 수신: room={}, sender={}", messageDto.getIdolId(), nickname);

        // 알림 로직은 ChatService로 이동됨
        chatService.processMessage(messageDto);
    }

    @MessageMapping("/chat/typing")
    public void typing(ChatMessageDto messageDto, SimpMessageHeaderAccessor accessor) {
        String role = (String) accessor.getSessionAttributes().get("role");
        if (!"IDOL".equals(role))
            return;

        Integer myIdolId = (Integer) accessor.getSessionAttributes().get("idolId");
        if (myIdolId == null || !myIdolId.equals(messageDto.getIdolId().intValue())) {
            return; // 다른 방에서 타이핑하는 척하는 것 차단
        }

        messageDto.setSenderRole(role);
        messageDto.setSenderId((Integer) accessor.getSessionAttributes().get("userId"));
        messageDto.setType("TYPING");
        redisTemplate.convertAndSend("/sub/idol/" + messageDto.getIdolId(), messageDto);

        log.debug("작성 중 신호 전송: room={}", messageDto.getIdolId());
    }

    // 채팅방 목록 조회 (Aggregation) - 추가됨
    @GetMapping("/chat/rooms")
    @ResponseBody
    public ResponseEntity<List<ChatRoomListDto>> getChatRoomList(@RequestHeader("X-User-Id") int userId) {
        return ResponseEntity.ok(chatService.getChatRoomList(userId));
    }

    // 읽음 처리 API - 추가됨
    @PostMapping("/chat/read/{idolId}")
    @ResponseBody
    public ResponseEntity<Void> markAsRead(
            @RequestHeader("X-User-Id") int userId,
            @PathVariable("idolId") Long idolId) {
        chatService.markAsRead(userId, idolId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/chat/history/{idolId}")
    @ResponseBody
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(
            @RequestHeader("X-User-Id") int userId, // userId 추가
            @RequestHeader(value = "X-Role", defaultValue = "USER") String role, // role 추가
            @PathVariable("idolId") Long idolId,
            @RequestParam(value = "lastId", required = false) String lastId,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        // 구독 권한 검증 (REST API 보안)
        chatService.validateSubscription(userId, idolId, role);
        return ResponseEntity.ok(chatService.getChatHistory(userId, role, idolId, lastId, size));
    }

    // 채팅방 미리보기 (마지막 메시지) 조회 API
    @GetMapping("/chat/preview/{idolId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getChatPreview(@PathVariable("idolId") Long idolId) {
        return ResponseEntity.ok(chatService.getChatPreview(idolId));
    }

    // --- 공지사항 API ---

    @PostMapping("/chat/pin")
    @ResponseBody
    public ResponseEntity<Void> pinMessage(
            @RequestHeader("X-Role") String role,
            @RequestBody Map<String, Object> request) {
        if (!"IDOL".equals(role) && !"ADMIN".equals(role) && !"AGENCY".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String messageId = (String) request.get("messageId");
        Long idolId = Long.valueOf(request.get("idolId").toString());

        chatService.pinMessage(messageId, idolId);
        return ResponseEntity.ok().build();
    }

    // 공지 해제
    @PostMapping("/chat/pin/remove")
    @ResponseBody
    public ResponseEntity<Void> unpinMessage(
            @RequestHeader("X-Role") String role,
            @RequestBody Map<String, Object> request) {
        if (!"IDOL".equals(role) && !"ADMIN".equals(role) && !"AGENCY".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long idolId = Long.valueOf(request.get("idolId").toString());

        chatService.unpinMessage(idolId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/chat/pin/{idolId}")
    @ResponseBody
    public ResponseEntity<ChatMessageDto> getPinnedMessage(@PathVariable("idolId") Long idolId) {
        return ResponseEntity.ok(chatService.getPinnedMessage(idolId));
    }

    // --- 미디어 모아보기 API (추가됨) ---

    @GetMapping("/chat/media/{idolId}")
    @ResponseBody
    public ResponseEntity<List<ChatMessageDto>> getChatMedia(
            @RequestHeader("X-User-Id") int userId, // userId 추가
            @RequestHeader(value = "X-Role", defaultValue = "USER") String role, // role 추가
            @PathVariable("idolId") Long idolId,
            @RequestParam(value = "lastId", required = false) String lastId,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        // 구독 권한 검증 (REST API 보안)
        chatService.validateSubscription(userId, idolId, role);
        return ResponseEntity.ok(chatService.getChatMedia(userId, role, idolId, lastId, size));
    }

    // --------------------

    // 파일 업로드 API 개선 (썸네일 포함)
    @PostMapping("/chat/upload")
    @ResponseBody
    public ResponseEntity<FileUploadResponseDto> uploadFile(@RequestParam("file") MultipartFile file) {
        FileUploadResponseDto response = s3Service.uploadFile(file);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat/message/delete")
    @ResponseBody
    public ResponseEntity<Void> deleteMessage(
            @RequestHeader("X-User-Id") int userId,
            @RequestBody Map<String, Object> request) {
        String messageId = (String) request.get("messageId");
        Long idolId = Long.valueOf(request.get("idolId").toString());

        log.info("메시지 삭제 요청: msgId={}, userId={}", messageId, userId);
        chatService.deleteMessage(messageId, idolId, userId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/chat/status/{idolId}")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> getIdolStatus(@PathVariable("idolId") Long idolId) {
        boolean isOnline = chatService.isIdolOnline(idolId);
        return ResponseEntity.ok(Map.of("online", isOnline));
    }

    @PostMapping("/chat/reaction")
    @ResponseBody
    public ResponseEntity<Void> addReaction(@RequestBody Map<String, Object> request) {
        String messageId = (String) request.get("messageId");
        String reactionType = (String) request.get("reactionType");
        Long idolId = Long.valueOf(request.get("idolId").toString());

        log.info("반응 추가 요청: msgId={}, type={}", messageId, reactionType);
        chatService.addReaction(messageId, reactionType, idolId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/chat/translate/{messageId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> translateMessage(
            @PathVariable("messageId") String messageId,
            @RequestParam(value = "lang", defaultValue = "EN") String lang) {
        String translatedText = translationService.translateMessage(messageId, lang);
        return ResponseEntity.ok(Map.of("text", translatedText, "lang", lang));
    }
}
