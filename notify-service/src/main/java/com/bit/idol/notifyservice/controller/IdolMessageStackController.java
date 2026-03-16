package com.bit.idol.notifyservice.controller;

import com.bit.idol.notifyservice.dto.IdolMessageStackListResponse;
import com.bit.idol.notifyservice.service.IdolMessageStackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notify")
public class IdolMessageStackController {

    private final IdolMessageStackService stackService;

    public IdolMessageStackController(IdolMessageStackService stackService) {
        this.stackService = stackService;
    }

    // 아이돌별 unread 목록(최근 메시지 온 아이돌이 위)
    @GetMapping("/idol-message-stacks")
    public ResponseEntity<IdolMessageStackListResponse> list(
            @RequestHeader("X-User-Id") int userId
    ) {
        return ResponseEntity.ok(stackService.list(userId));
    }

    // 채팅방 들어갈 때 호출: 해당 idolId 스택 0으로
    @PostMapping("/idol-message-stacks/{idolId}/reset")
    public ResponseEntity<Void> reset(
            @RequestHeader("X-User-Id") int userId,
            @PathVariable("idolId") long idolId
    ) {
        stackService.reset(userId, idolId);
        return ResponseEntity.ok().build();
    }

    // 전체 스택 unread 초기화
    @PostMapping("/idol-message-stacks/reset-all")
    public ResponseEntity<Void> resetAll(
            @RequestHeader("X-User-Id") int userId
    ) {
        stackService.resetAll(userId);
        return ResponseEntity.ok().build();
    }
}
