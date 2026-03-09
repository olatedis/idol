package com.bit.idol.userservice.controller;

import com.bit.idol.userservice.dto.agency.AgencyCreateRequest;
import com.bit.idol.userservice.dto.agency.AgencyDto;
import com.bit.idol.userservice.dto.agency.AgencyUpdateRequest;
import com.bit.idol.userservice.entity.Role;
import com.bit.idol.userservice.service.AgencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agencies")
@RequiredArgsConstructor
@Slf4j
public class AgencyController {

    private final AgencyService agencyService;

    // 소속사 등록
    @PostMapping
    public ResponseEntity<AgencyDto> createAgency(
            @RequestHeader("X-Role") String role,
            @Valid @RequestBody AgencyCreateRequest request) {
        Role requesterRole = Role.valueOf(role);
        // ADMIN만 가능
        if (!(requesterRole == Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agencyService.createAgency(request));
    }

    // 소속사 전체 목록
    @GetMapping
    public ResponseEntity<List<AgencyDto>> getAllAgencies() {
        return ResponseEntity.ok(agencyService.getAllAgencies());
    }

    // 소속사 수정
    @PostMapping("/{agencyId}/update")
    public ResponseEntity<AgencyDto> updateAgency(
            @RequestHeader("X-Role") String role,
            @PathVariable int agencyId,
            @Valid @RequestBody AgencyUpdateRequest request) {
        Role requesterRole = Role.valueOf(role);
        if (requesterRole != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(agencyService.updateAgency(agencyId, request));
    }

    // 소속사 삭제
    @PostMapping("/{agencyId}/delete")
    public ResponseEntity<Void> deleteAgency(
            @RequestHeader("X-Role") String role,
            @PathVariable int agencyId) {
        Role requesterRole = Role.valueOf(role);
        if (requesterRole != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        agencyService.deleteAgency(agencyId);
        return ResponseEntity.noContent().build();
    }
}
