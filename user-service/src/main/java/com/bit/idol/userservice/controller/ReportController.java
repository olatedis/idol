package com.bit.idol.userservice.controller;

import com.bit.idol.userservice.dto.report.ReportRequestDto;
import com.bit.idol.userservice.dto.report.UserStatusChangeDto;
import com.bit.idol.userservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    // 신고하기 (일반 유저)
    @PostMapping("/reports")
    public ResponseEntity<String> reportUser(
            @RequestHeader("X-User-Id") int reporterId,
            @RequestBody ReportRequestDto requestDto) {
        
        reportService.reportUser(reporterId, requestDto);
        return ResponseEntity.ok("신고가 접수되었습니다.");
    }

    // 유저 상태 변경 (관리자 전용)
    @PostMapping("/admin/users/status")
    public ResponseEntity<String> changeUserStatus(
            @RequestHeader("X-Role") String role,
            @RequestBody UserStatusChangeDto requestDto) {

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("관리자 권한이 필요합니다.");
        }

        reportService.changeUserStatus(requestDto);
        return ResponseEntity.ok("유저 상태가 변경되었습니다.");
    }
}
