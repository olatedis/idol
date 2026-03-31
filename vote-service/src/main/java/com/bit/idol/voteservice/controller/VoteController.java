package com.bit.idol.voteservice.controller;

import com.bit.idol.voteservice.dto.*;
import com.bit.idol.voteservice.service.VoteReader;
import com.bit.idol.voteservice.service.VoteService;
import com.bit.idol.voteservice.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final VoteReader voteReader;

    // 투표 생성 (ADMIN 추가됨)
    @PostMapping
    public ResponseEntity<?> createVote(
            @RequestHeader("X-Role") String role,
            @RequestBody @Valid CreateVoteRequestDto requestDto) {

        if (!"IDOL".equals(role) && !"AGENCY".equals(role) && !"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("투표 생성 권한이 없습니다.");
        }

        VoteInfo voteInfo = voteService.createVote(requestDto.toEntity());
        return ResponseEntity.ok(voteInfo);
    }

    // 투표 목록 조회 (기존: 검색/페이징용 - 비로그인 가능)
    @GetMapping
    public ResponseEntity<java.util.Map<String, Object>> getVoteList(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String keyword,
            @org.springframework.data.web.PageableDefault(size = 10, sort = "startDate", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<VoteInfo> page = voteReader.getVoteList(groupId, keyword, pageable);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("content", page.getContent());
        response.put("totalPages", page.getTotalPages());
        response.put("totalElements", page.getTotalElements());
        response.put("size", page.getSize());
        response.put("number", page.getNumber());
        
        return ResponseEntity.ok(response);
    }

    // 투표 목록 조회 (로그인 유저용 - 내 참여 여부 포함)
    @GetMapping("/list")
    public ResponseEntity<List<VoteListDto>> getVoteListWithStatus(
            @RequestHeader("X-User-Id") int userId,
            @RequestParam(required = false) Long groupId) { // 그룹 ID 필터 추가
        return ResponseEntity.ok(voteService.getVoteList(userId, groupId));
    }


    // 내 투표 기록 조회
    @GetMapping("/me")
    public ResponseEntity<List<MyVoteRecordDto>> getMyVoteRecords(
            @RequestHeader("X-User-Id") int userId,
            @RequestParam(required = false) Long groupId) {
        List<MyVoteRecordDto> records = voteService.getMyVoteRecords(userId, groupId);
        return ResponseEntity.ok(records);
    }

    // 투표 상세 조회 - userId 추가 (비로그인 허용)
    @GetMapping("/{voteId}")
    public ResponseEntity<VoteDetailDto> getVoteDetail(
            @PathVariable int voteId,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {

        VoteDetailDto voteDetail = voteReader.getVoteDetail(voteId, userId);
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
            @RequestBody @Valid VoteRequestDto requestDto,
            HttpServletRequest request) {

        if (!"USER".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("투표 권한이 없습니다. 일반 유저만 투표 가능합니다.");
        }

        String clientIp = IpUtils.getClientIp(request);

        String result = voteService.castVote(
                voteId,
                userId,
                requestDto.getCandidateNumber(),
                clientIp);

        return ResponseEntity.ok(result);
    }

    // 투표 취소
    @PostMapping("/{voteId}/cancel")
    public ResponseEntity<String> cancelVote(
            @PathVariable int voteId,
            @RequestHeader("X-User-Id") int userId) {

        voteService.cancelVote(voteId, userId);
        return ResponseEntity.ok("투표가 취소되었습니다.");
    }

    // 투표 삭제 (ADMIN, AGENCY, IDOL 만 가능)
    @DeleteMapping("/{voteId}")
    public ResponseEntity<?> deleteVote(
            @PathVariable int voteId,
            @RequestHeader("X-Role") String role) {

        if (!"ADMIN".equals(role) && !"AGENCY".equals(role) && !"IDOL".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("투표 삭제 권한이 없습니다.");
        }

        voteService.deleteVote(voteId);
        return ResponseEntity.ok("투표가 삭제되었습니다.");
    }
}
