package com.bit.idol.chatservice.controller;

import com.bit.idol.chatservice.dto.ChatMessageDto;
import com.bit.idol.chatservice.service.ChatService;
import com.bit.idol.chatservice.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        // 아이돌만 사용 가능
        String role = (String) accessor.getSessionAttributes().get("role");
        if (!"IDOL".equals(role)) return;

        // Kafka나 Redis를 거치지 않고 바로 WebSocket 브로커로 쏘는 게 효율적이지만,
        // 서버가 여러 대일 수 있으므로 Redis Pub/Sub을 타야 함.
        // 여기서는 간단히 ChatService에 위임하지 않고 바로 RedisTemplate을 써도 되지만,
        // 구조 통일을 위해 ChatService에 typing 메서드를 만드는 게 좋음.
        // (일단은 생략하고 클라이언트가 알아서 처리하도록 두거나, 추후 구현)
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
}
