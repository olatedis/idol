package com.bit.idol.chatservice.controller;

import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.bit.idol.chatservice.service.ChatService;
import com.bit.idol.chatservice.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final S3Service s3Service;
    private final RedisTemplate<String, Object> redisTemplate;

    // 클라이언트가 /pub/chat/send 로 메시지를 보내면 여기서 처리
    @MessageMapping("/chat/send")
    public void sendMessage(ChatMessageDto messageDto, SimpMessageHeaderAccessor accessor) {
        // 세션에서 인증된 유저 정보 가져오기 (위변조 방지)
        Integer userId = (Integer) accessor.getSessionAttributes().get("userId");
        String role = (String) accessor.getSessionAttributes().get("role");
        String nickname = (String) accessor.getSessionAttributes().get("nickname");

        if (userId == null) {
            log.error("인증된 유저 정보가 없습니다.");
            return;
        }

        // DTO에 세션 정보 덮어쓰기
        messageDto.setSenderId(userId);
        messageDto.setSenderRole(role);
        messageDto.setSenderNickname(nickname);

        log.info("메시지 수신: room={}, sender={}", messageDto.getIdolId(), nickname);
        
        // 서비스 로직 실행 (검열 -> 저장 -> Kafka 발행)
        chatService.processMessage(messageDto);
    }

    // 작성 중 표시 (저장 X, 브로드캐스팅만)
    @MessageMapping("/chat/typing")
    public void typing(ChatMessageDto messageDto, SimpMessageHeaderAccessor accessor) {
        // 아이돌만 사용 가능
        String role = (String) accessor.getSessionAttributes().get("role");
        if (!"IDOL".equals(role)) return;

        // Redis Pub/Sub으로 바로 발행 (DB 저장 X, Kafka X)
        // 팬들은 /sub/idol/{id}를 구독 중이므로 거기로 쏘면 됨
        // 타입만 TYPING으로 변경
        messageDto.setType("TYPING");
        redisTemplate.convertAndSend("/sub/idol/" + messageDto.getIdolId(), messageDto);
        
        log.debug("작성 중 신호 전송: room={}", messageDto.getIdolId());
    }

    // 채팅 내역 조회 API (HTTP)
    @GetMapping("/chat/history/{idolId}")
    @ResponseBody
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(
            @PathVariable("idolId") Long idolId,
            @RequestParam(value = "lastId", required = false) String lastId,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(chatService.getChatHistory(idolId, lastId, size));
    }

    // 파일 업로드 API (HTTP)
    @PostMapping("/chat/upload")
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = s3Service.uploadFile(file);
        String contentType = file.getContentType();
        String type = "FILE";

        if (contentType != null) {
            if (contentType.startsWith("image")) {
                type = "IMAGE";
            } else if (contentType.startsWith("video")) {
                type = "VIDEO";
            } else if (contentType.startsWith("audio")) {
                type = "VOICE";
            }
        }

        Map<String, String> response = new HashMap<>();
        response.put("url", fileUrl);
        response.put("type", type);
        
        return ResponseEntity.ok(response);
    }

    // 메시지 삭제 API (HTTP POST)
    // 본인이 보낸 메시지만 삭제 가능 (Soft Delete)
    @PostMapping("/chat/message/delete")
    @ResponseBody
    public ResponseEntity<Void> deleteMessage(
            @RequestHeader("X-User-Id") int userId, // Gateway에서 넘어온 유저 ID
            @RequestBody Map<String, Object> request
    ) {
        String messageId = (String) request.get("messageId");
        Long idolId = Long.valueOf(request.get("idolId").toString());

        log.info("메시지 삭제 요청: msgId={}, userId={}", messageId, userId);
        chatService.deleteMessage(messageId, idolId, userId);
        
        return ResponseEntity.ok().build();
    }

    // 아이돌 접속 상태 확인 API (HTTP GET)
    // 채팅방 목록에서 초록불(🟢) 띄울 때 사용
    @GetMapping("/chat/status/{idolId}")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> getIdolStatus(@PathVariable("idolId") Long idolId) {
        boolean isOnline = chatService.isIdolOnline(idolId);
        return ResponseEntity.ok(Map.of("online", isOnline));
    }

    // 메시지 반응 추가 API (HTTP POST)
    // 좋아요, 하트 등 이모지 반응
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
}
