package com.bit.idol.userservice.controller;

import com.bit.idol.userservice.dto.report.ReportRequestDto;
import com.bit.idol.userservice.dto.report.UserStatusChangeDto;
import com.bit.idol.userservice.dto.report.ReportDto;
import com.bit.idol.userservice.dto.user.BanHistoryDto;
import com.bit.idol.userservice.dto.user.UserDto;
import com.bit.idol.userservice.service.ReportService;
import com.bit.idol.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

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

    // 관리자: 대기열 조회
    @GetMapping("/admin/users/reports")
    public ResponseEntity<List<UserDto>> getActiveUsersWithReports(
            @RequestHeader("X-Role") String role) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(reportService.getActiveUsersWithReports());
    }

    // 관리자: 유저 검색
    @GetMapping("/admin/users/search")
    public ResponseEntity<List<UserDto>> searchUsersForAdmin(
            @RequestHeader("X-Role") String role,
            @RequestParam("keyword") String keyword) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(reportService.searchUsersForAdmin(keyword));
    }

    // 관리자: 전체 유저 페이징 조회
    @GetMapping("/admin/users")
    public ResponseEntity<Page<UserDto>> getAllUsersForAdmin(
            @RequestHeader("X-Role") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return ResponseEntity.ok(userService.getAllUsersWithPaging(pageable));
    }

    // 관리자: 특정 유저 피신고 이력 조회
    @GetMapping("/admin/users/{userId}/reports-history")
    public ResponseEntity<List<ReportDto>> getUserReportHistory(
            @RequestHeader("X-Role") String role,
            @PathVariable("userId") int targetUserId) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(reportService.getUserReportHistory(targetUserId));
    }

    // 관리자: 특정 유저 제재 이력 조회
    @GetMapping("/admin/users/{userId}/bans-history")
    public ResponseEntity<List<BanHistoryDto>> getUserBanHistoryForAdmin(
            @RequestHeader("X-Role") String role,
            @PathVariable("userId") int targetUserId) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userService.getUserBanHistory(targetUserId));
    }
}
