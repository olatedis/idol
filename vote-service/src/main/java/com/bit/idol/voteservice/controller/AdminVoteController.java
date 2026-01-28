package com.bit.idol.voteservice.controller;

import com.bit.idol.voteservice.service.VoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/votes")
@RequiredArgsConstructor
@Slf4j
public class AdminVoteController {

    private final VoteService voteService;

    // 블랙리스트 IP 추가
    @PostMapping("/blacklist/add")
    public ResponseEntity<String> addBlacklistIp(
            @RequestHeader("X-Role") String role,
            @RequestBody Map<String, String> request) {
        
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("접근 권한이 없습니다.");
        }

        String ip = request.get("ip");
        if (ip == null || ip.isEmpty()) {
            return ResponseEntity.badRequest().body("IP 주소는 필수입니다.");
        }

        voteService.addBlacklistIp(ip);
        return ResponseEntity.ok("IP가 블랙리스트에 추가되었습니다: " + ip);
    }

    // 블랙리스트 IP 해제
    @PostMapping("/blacklist/remove")
    public ResponseEntity<String> removeBlacklistIp(
            @RequestHeader("X-Role") String role,
            @RequestBody Map<String, String> request) {
        
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("접근 권한이 없습니다.");
        }

        String ip = request.get("ip");
        if (ip == null || ip.isEmpty()) {
            return ResponseEntity.badRequest().body("IP 주소는 필수입니다.");
        }

        voteService.removeBlacklistIp(ip);
        return ResponseEntity.ok("IP가 블랙리스트에서 해제되었습니다: " + ip);
    }
}
