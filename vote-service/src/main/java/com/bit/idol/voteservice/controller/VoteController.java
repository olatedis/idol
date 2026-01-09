package com.bit.idol.voteservice.controller;

import com.bit.idol.voteservice.dto.VoteRequestDto;
import com.bit.idol.voteservice.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/votes")
@RequiredArgsConstructor

public class VoteController {

    private final VoteService voteService;

    @PostMapping("/{voteId}")
    public ResponseEntity<String> castVote(
            @PathVariable int voteId,
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role, // 헤더에서 권한 추출
            @RequestBody VoteRequestDto requestDto) {

        // 1. 권한 체크: 'USER'가 아니면 투표 불가
        if (!"USER".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("투표 권한이 없습니다. 일반 유저만 투표 가능합니다.");
        }

        // 2. 권한 통과 시 서비스 호출
        String result = voteService.castVote(
                voteId,
                userId,
                requestDto.getCandidateNumber()
        );

        return ResponseEntity.ok(result);
    }
}
