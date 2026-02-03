package com.bit.idol.userservice.controller;

import com.bit.idol.userservice.dto.idol.IdolDto;
import com.bit.idol.userservice.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    // 그룹 소속 아이돌 목록 조회
    @GetMapping("/{groupId}/idols")
    public ResponseEntity<List<IdolDto>> getIdolsByGroup(@PathVariable int groupId) {
        return ResponseEntity.ok(groupService.getIdolsByGroup(groupId));
    }
}
