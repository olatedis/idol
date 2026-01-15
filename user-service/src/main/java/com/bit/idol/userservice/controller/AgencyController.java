package com.bit.idol.userservice.controller;

import com.bit.idol.userservice.dto.agency.AgencyCreateRequest;
import com.bit.idol.userservice.dto.agency.AgencyDto;
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

    @PostMapping
    public ResponseEntity<AgencyDto> createAgency(
            @RequestHeader("X-Role") String role,
            @Valid @RequestBody AgencyCreateRequest request
    ) {
        Role requesterRole = Role.valueOf(role);
        if (!(requesterRole == Role.ADMIN || requesterRole == Role.AGENCY)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agencyService.createAgency(request));
    }

    @GetMapping("/{agencyId}")
    public ResponseEntity<AgencyDto> getAgency(@PathVariable int agencyId) {
        return ResponseEntity.ok(agencyService.getAgency(agencyId));
    }

    @GetMapping
    public ResponseEntity<List<AgencyDto>> getAllAgencies() {
        return ResponseEntity.ok(agencyService.getAllAgencies());
    }
}

