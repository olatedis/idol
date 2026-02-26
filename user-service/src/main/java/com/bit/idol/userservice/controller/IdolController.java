package com.bit.idol.userservice.controller;

import com.bit.idol.userservice.dto.idol.IdolDto;
import com.bit.idol.userservice.dto.idol.IdolRegisterRequest;
import com.bit.idol.userservice.dto.idol.IdolStatusChangeRequest;
import com.bit.idol.userservice.entity.Role;
import com.bit.idol.userservice.service.IdolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/idols")
@RequiredArgsConstructor
@Slf4j
public class IdolController {

    private final IdolService idolService;

    // 아이돌 등록
    @PostMapping
    public ResponseEntity<IdolDto> registerIdol(
            @RequestHeader("X-Role") String role,
            @Valid @RequestBody IdolRegisterRequest request) {
        // 권한 확인. ADMIN과 AGENCY만 등록할 수 있다.
        Role requesterRole = Role.valueOf(role);
        if (!(requesterRole == Role.ADMIN || requesterRole == Role.AGENCY)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(idolService.registerIdol(request));
    }

    // 내 아이돌 정보 검색 (현재 로그인한 유저 기준)
    @GetMapping("/me")
    public ResponseEntity<IdolDto> getMyIdolInfo(@RequestHeader("X-User-Id") int userId) {
        return ResponseEntity.ok(idolService.getIdolByUserId(userId));
    }

    // 아이돌 검색
    @GetMapping("/{idolId}")
    public ResponseEntity<IdolDto> getIdol(@PathVariable int idolId) {
        return ResponseEntity.ok(idolService.getIdol(idolId));
    }

    // 아이돌 전체 목록
    @GetMapping
    public ResponseEntity<List<IdolDto>> getAllIdols() {
        return ResponseEntity.ok(idolService.getAllIdols());
    }

    // 아이돌 상태 변경
    @PostMapping("/status/{idolId}")
    public ResponseEntity<Void> changeIdolStatus(
            @PathVariable int idolId,
            @RequestHeader("X-Role") String role,
            @RequestBody IdolStatusChangeRequest request) {
        Role requesterRole = Role.valueOf(role);
        // ADMIN과 AGENCY만 가능
        if (!(requesterRole == Role.ADMIN || requesterRole == Role.AGENCY)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        idolService.changeIdolStatus(idolId, request.getStatus());
        return ResponseEntity.ok().build();
    }
}
