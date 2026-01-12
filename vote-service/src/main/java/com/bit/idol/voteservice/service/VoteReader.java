package com.bit.idol.voteservice.service;

import com.bit.idol.voteservice.dto.VoteDetailDto;
import com.bit.idol.voteservice.dto.VoteInfo;
import com.bit.idol.voteservice.entity.Vote;
import com.bit.idol.voteservice.repository.VoteRecordRepository;
import com.bit.idol.voteservice.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteReader {

    private final VoteRepository voteRepository;
    private final VoteRecordRepository voteRecordRepository;

    // 투표 정보 조회 (캐싱 적용, sync=true로 Cache Stampede 방지)
    // 주로 내부 로직(투표 참여 등)에서 사용
    @Cacheable(value = "voteInfo", key = "#voteId", sync = true)
    public VoteInfo getVoteInfo(int voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 투표입니다."));
        return VoteInfo.from(vote);
    }

    // 투표 목록 조회 (페이징)
    public Page<VoteInfo> getVoteList(Pageable pageable) {
        return voteRepository.findAll(pageable)
                .map(VoteInfo::from);
    }

    // 투표 상세 조회 (후보자 목록 포함)
    public VoteDetailDto getVoteDetail(int voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 투표입니다."));
        return VoteDetailDto.from(vote);
    }

    // 투표 참여 여부 확인
    public boolean hasVoted(int voteId, int userId) {
        return voteRecordRepository.existsByVoteIdAndUserId(voteId, userId);
    }
}
