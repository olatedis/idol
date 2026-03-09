package com.bit.idol.notifyservice.controller;

import com.bit.idol.notifyservice.dto.PreferenceResponse;
import com.bit.idol.notifyservice.dto.UpdatePreferenceRequest;
import com.bit.idol.notifyservice.service.PreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notify/preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public ResponseEntity<PreferenceResponse> get(
            @RequestHeader("X-User-Id") int userId
    ) {
        return ResponseEntity.ok(preferenceService.getOrCreate(userId));
    }

    @PutMapping
    public ResponseEntity<PreferenceResponse> update(
            @RequestHeader("X-User-Id") int userId,
            @RequestBody UpdatePreferenceRequest req
    ) {
        return ResponseEntity.ok(preferenceService.update(userId, req));
    }
}