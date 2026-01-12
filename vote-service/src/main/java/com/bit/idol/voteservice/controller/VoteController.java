package com.bit.idol.voteservice.controller;

import com.bit.idol.voteservice.dto.CreateVoteRequestDto;
import com.bit.idol.voteservice.dto.VoteDetailDto;
import com.bit.idol.voteservice.dto.VoteInfo;
import com.bit.idol.voteservice.dto.VoteRequestDto;
import com.bit.idol.voteservice.service.VoteReader;
import com.bit.idol.voteservice.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final VoteReader voteReader;

    // 투표 생성
    @PostMapping
    public ResponseEntity<?> createVote(
            @RequestHeader("X-Role") String role,
            @RequestBody CreateVoteRequestDto requestDto) {

        // 권한 체크 (IDOL 또는 AGENCY만 가능)
        if (!"IDOL".equals(role) && !"AGENCY".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("투표 생성 권한이 없습니다.");
        }

        VoteInfo voteInfo = voteService.createVote(requestDto.toEntity());
        return ResponseEntity.ok(voteInfo);
    }

    // 투표 목록 조회 (페이징)
    @GetMapping
    public ResponseEntity<Page<VoteInfo>> getVoteList(
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<VoteInfo> voteList = voteReader.getVoteList(pageable);
        return ResponseEntity.ok(voteList);
    }

    // 투표 상세 조회
    @GetMapping("/{voteId}")
    public ResponseEntity<VoteDetailDto> getVoteDetail(@PathVariable int voteId) {
        VoteDetailDto voteDetail = voteReader.getVoteDetail(voteId);
        return ResponseEntity.ok(voteDetail);
    }

    // 투표 참여 여부 확인
    @GetMapping("/{voteId}/check")
    public ResponseEntity<Boolean> checkVoteStatus(
            @PathVariable int voteId,
            @RequestHeader("X-User-Id") int userId) {
        boolean hasVoted = voteReader.hasVoted(voteId, userId);
        return ResponseEntity.ok(hasVoted);
    }

    // 투표 참여
    @PostMapping("/{voteId}")
    public ResponseEntity<String> castVote(
            @PathVariable int voteId,
            @RequestHeader("X-User-Id") int userId,
            @RequestHeader("X-Role") String role,
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
