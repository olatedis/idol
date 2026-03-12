package com.bit.idol.userservice.controller;

import com.bit.idol.userservice.dto.group.GroupDto;
import com.bit.idol.userservice.dto.idol.IdolDto;
import com.bit.idol.userservice.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.bit.idol.userservice.service.InternalValidationService;

import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final InternalValidationService internalValidationService;

    // 그룹 상세 조회 (추가됨)
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDto> getGroup(@PathVariable int groupId) {
        return ResponseEntity.ok(groupService.getGroup(groupId));
    }

    // 전체 그룹 목록 조회 (추가됨)
    @GetMapping
    public ResponseEntity<List<GroupDto>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    // 에이전시 관리 그룹 목록 조회
    @GetMapping("/managed")
    public ResponseEntity<List<GroupDto>> getManagedGroups(
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader(value = "X-Role", defaultValue = "USER") String role) {
        if (!"AGENCY".equals(role)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(groupService.getManagedGroups(userId));
    }

    // 그룹 소속 아이돌 목록 조회 (기존 메서드 유지)
    @GetMapping("/{groupId}/idols")
    public ResponseEntity<List<IdolDto>> getIdolsByGroup(@PathVariable int groupId) {
        return ResponseEntity.ok(groupService.getIdolsByGroup(groupId));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<Void> addMemberToGroup(
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader(value = "X-Role", defaultValue = "USER") String role,
            @PathVariable int groupId,
            @RequestParam int idolId) {

        if (!"AGENCY".equals(role)) {
            return ResponseEntity.status(403).build();
        }
        
        if (!internalValidationService.canAgencyManageGroup(userId, groupId)) {
            return ResponseEntity.status(403).build();
        }

        groupService.addMemberToGroup(groupId, idolId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/members/remove")
    public ResponseEntity<Void> removeMemberFromGroup(
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader(value = "X-Role", defaultValue = "USER") String role,
            @PathVariable int groupId,
            @RequestParam int idolId) {

        if (!"AGENCY".equals(role)) {
            return ResponseEntity.status(403).build();
        }

        if (!internalValidationService.canAgencyManageGroup(userId, groupId)) {
            return ResponseEntity.status(403).build();
        }

        groupService.removeMemberFromGroup(groupId, idolId);
        return ResponseEntity.ok().build();
    }
}
