package com.bit.idol.notifyservice.controller;

import com.bit.idol.notifyservice.dto.IdolMessageStackListResponse;
import com.bit.idol.notifyservice.service.IdolMessageStackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notify")
public class IdolMessageStackController {

    private final IdolMessageStackService service;

    public IdolMessageStackController(IdolMessageStackService service) {
        this.service = service;
    }

    // 유저별 아이돌 메시지 목록(최근순)
    @GetMapping("/idol-message-stacks")
    public ResponseEntity<IdolMessageStackListResponse> list(
            @RequestHeader("X-User-Id") int userId
    ) {
        return ResponseEntity.ok(service.list(userId));
    }

    // 특정 idolId 읽음 처리(스택 0으로)
    @PostMapping("/idol-message-stacks/{idolId}/read")
    public ResponseEntity<?> markRead(
            @RequestHeader("X-User-Id") int userId,
            @PathVariable("idolId") long idolId
    ) {
        service.markRead(userId, idolId);
        return ResponseEntity.ok().build();
    }
}
