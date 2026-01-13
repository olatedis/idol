package com.bit.idol.rankingservice.controller;

import com.bit.idol.rankingservice.dto.RankingDto;
import com.bit.idol.rankingservice.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    // 초기 랭킹 조회 (페이지 로딩 시 호출)
    @GetMapping("/{voteId}")
    public ResponseEntity<List<RankingDto>> getRanking(@PathVariable int voteId) {
        List<RankingDto> rankingList = rankingService.getRanking(voteId);
        return ResponseEntity.ok(rankingList);
    }
}
