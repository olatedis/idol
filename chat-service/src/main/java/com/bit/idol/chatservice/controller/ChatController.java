package com.bit.idol.chatservice.controller;

import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.bit.idol.chatservice.service.ChatService;
import com.bit.idol.chatservice.service.S3Service;
import com.bit.idol.chatservice.service.TranslationService;
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
    private final TranslationService translationService; // 번역 서비스 추가
    private final RedisTemplate<String, Object> redisTemplate;

    // 클라이언트가 /pub/chat/send 로 메시지를 보내면 여기서 처리
    @MessageMapping("/chat/send")
    public void sendMessage(ChatMessageDto messageDto, SimpMessageHeaderAccessor accessor) {
        Integer userId = (Integer) accessor.getSessionAttributes().get("userId");
        String role = (String) accessor.getSessionAttributes().get("role");
        String nickname = (String) accessor.getSessionAttributes().get("nickname");

        if (userId == null) {
            log.error("인증된 유저 정보가 없습니다.");
            return;
        }

        messageDto.setSenderId(userId);
        messageDto.setSenderRole(role);
        messageDto.setSenderNickname(nickname);

        log.info("메시지 수신: room={}, sender={}", messageDto.getIdolId(), nickname);
        
        chatService.processMessage(messageDto);
    }

    // 작성 중 표시 (저장 X, 브로드캐스팅만)
    @MessageMapping("/chat/typing")
    public void typing(ChatMessageDto messageDto, SimpMessageHeaderAccessor accessor) {
        String role = (String) accessor.getSessionAttributes().get("role");
        if (!"IDOL".equals(role)) return;

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
    @PostMapping("/chat/message/delete")
    @ResponseBody
    public ResponseEntity<Void> deleteMessage(
            @RequestHeader("X-User-Id") int userId,
            @RequestBody Map<String, Object> request
    ) {
        String messageId = (String) request.get("messageId");
        Long idolId = Long.valueOf(request.get("idolId").toString());

        log.info("메시지 삭제 요청: msgId={}, userId={}", messageId, userId);
        chatService.deleteMessage(messageId, idolId, userId);
        
        return ResponseEntity.ok().build();
    }

    // 아이돌 접속 상태 확인 API (HTTP GET)
    @GetMapping("/chat/status/{idolId}")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> getIdolStatus(@PathVariable("idolId") Long idolId) {
        boolean isOnline = chatService.isIdolOnline(idolId);
        return ResponseEntity.ok(Map.of("online", isOnline));
    }

    // 메시지 반응 추가 API (HTTP POST)
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

    // 메시지 번역 API (HTTP GET)
    // 예: /chat/translate/msg123?lang=EN
    @GetMapping("/chat/translate/{messageId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> translateMessage(
            @PathVariable("messageId") String messageId,
            @RequestParam(value = "lang", defaultValue = "EN") String lang
    ) {
        String translatedText = translationService.translateMessage(messageId, lang);
        return ResponseEntity.ok(Map.of("text", translatedText, "lang", lang));
    }
}
