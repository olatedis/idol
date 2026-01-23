package com.bit.idol.userservice.controller;

import com.bit.idol.userservice.dto.user.UserDto;
import com.bit.idol.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.bit.idol.userservice.service.InternalValidationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Slf4j
public class InternalUserController {

    private final UserService userService;
    private final InternalValidationService internalValidationService;


    @GetMapping("/info/{username}")
    public ResponseEntity<UserDto> getUserInfo(@PathVariable("username") String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @GetMapping("/info/id/{userId}")
    public ResponseEntity<UserDto> getUserInfoById(@PathVariable("userId") int userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }
    // ========================================================================

    // Fanout Service용 전체 유저 조회
    @GetMapping("/info/all")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // ========================================================================

    // Idol 본인 검증
    @GetMapping("/idols/{idolId}/owners/{userId}")
    public ResponseEntity<Boolean> isIdolOwner(
            @PathVariable("idolId") int idolId,
            @PathVariable("userId") int userId
    ) {
        return ResponseEntity.ok(internalValidationService.isIdolOwner(idolId, userId));
    }

    // Agency가 idol 관리 가능한지
    @GetMapping("/agencies/users/{agencyUserId}/idols/{idolId}/manageable")
    public ResponseEntity<Boolean> canAgencyManageIdol(
            @PathVariable("agencyUserId") int agencyUserId,
            @PathVariable("idolId") int idolId
    ) {
        return ResponseEntity.ok(internalValidationService.canAgencyManageIdol(agencyUserId, idolId));
    }

    // Group 멤버 여부
    @GetMapping("/groups/{groupId}/members/{userId}/exists")
    public ResponseEntity<Boolean> isGroupMember(
            @PathVariable("groupId") int groupId,
            @PathVariable("userId") int userId
    ) {
        return ResponseEntity.ok(internalValidationService.isGroupMember(groupId, userId));
    }

    // Agency가 group 관리 가능한지
    @GetMapping("/agencies/users/{agencyUserId}/groups/{groupId}/manageable")
    public ResponseEntity<Boolean> canAgencyManageGroup(
            @PathVariable("agencyUserId") int agencyUserId,
            @PathVariable("groupId") int groupId
    ) {
        return ResponseEntity.ok(internalValidationService.canAgencyManageGroup(agencyUserId, groupId));
    }

    // ========================================================================

    // 비밀번호 재설정 (Auth Service에서 호출) - 반환값 Integer로 변경
    @PostMapping("/password/reset")
    public ResponseEntity<Integer> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");
        
        log.info("비밀번호 재설정 요청 (Internal): email={}", email);
        int userId = userService.resetPassword(email, newPassword);
        
        return ResponseEntity.ok(userId);
    }

    // 신고 횟수 증가 (Chat Service 등에서 호출)
    @PostMapping("/{userId}/report")
    public ResponseEntity<Void> increaseReportCount(@PathVariable("userId") int userId) {
        log.info("유저 신고 요청 (Internal): userId={}", userId);
        userService.increaseReportCount(userId);
        return ResponseEntity.ok().build();
    }
}
