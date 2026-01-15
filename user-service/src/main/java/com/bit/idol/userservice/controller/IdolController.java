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

    @PostMapping
    public ResponseEntity<IdolDto> registerIdol(
            @RequestHeader("X-Role") String role,
            @Valid @RequestBody IdolRegisterRequest request
    ) {
        Role requesterRole = Role.valueOf(role);
        if (!(requesterRole == Role.ADMIN || requesterRole == Role.AGENCY)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(idolService.registerIdol(request));
    }

    @GetMapping("/{idolId}")
    public ResponseEntity<IdolDto> getIdol(@PathVariable int idolId) {
        return ResponseEntity.ok(idolService.getIdol(idolId));
    }

    @GetMapping
    public ResponseEntity<List<IdolDto>> getAllIdols() {
        return ResponseEntity.ok(idolService.getAllIdols());
    }

    @PostMapping("/status/{idolId}")
    public ResponseEntity<Void> changeIdolStatus(
            @PathVariable int idolId,
            @RequestHeader("X-Role") String role,
            @RequestBody IdolStatusChangeRequest request
    ) {
        Role requesterRole = Role.valueOf(role);
        if (!(requesterRole == Role.ADMIN || requesterRole == Role.AGENCY)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        idolService.changeIdolStatus(idolId, request.getStatus());
        return ResponseEntity.ok().build();
    }
}

