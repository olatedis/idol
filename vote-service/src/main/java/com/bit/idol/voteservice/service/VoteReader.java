package com.bit.idol.voteservice.service;

import com.bit.idol.voteservice.dto.VoteDetailDto;
import com.bit.idol.voteservice.dto.VoteInfo;
import com.bit.idol.voteservice.entity.Candidate;
import com.bit.idol.voteservice.entity.Vote;
import com.bit.idol.voteservice.repository.CandidateRepository;
import com.bit.idol.voteservice.repository.VoteRecordRepository;
import com.bit.idol.voteservice.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteReader {

    private final VoteRepository voteRepository;
    private final VoteRecordRepository voteRecordRepository;
    private final CandidateRepository candidateRepository;

    // 투표 정보 조회 (캐싱 적용, sync=true로 Cache Stampede 방지)
    @Cacheable(value = "voteInfo", key = "#voteId", sync = true)
    public VoteInfo getVoteInfo(int voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 투표입니다."));
        return VoteInfo.from(vote);
    }

    // 후보자 정보 조회 (캐싱 적용 - 배치 처리 최적화용)
    @Cacheable(value = "candidateInfo", key = "#voteId + ':' + #candidateNumber", unless = "#result == null")
    public Candidate getCandidate(int voteId, int candidateNumber) {
        return candidateRepository.findByVoteIdAndCandidateNumber(voteId, candidateNumber)
                .orElseThrow(() -> new RuntimeException("후보자 없음"));
    }

    // 전체 투표 목록 조회 (캐싱 적용) - 추가됨
    @Cacheable(value = "votes", key = "'all'")
    public List<Vote> getAllVotesCached() {
        return voteRepository.findAll();
    }

    // 투표 목록 조회 (페이징 + 검색)
    public Page<VoteInfo> getVoteList(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return voteRepository.findAll(pageable)
                    .map(VoteInfo::from);
        }
        return voteRepository.findByTitleContaining(keyword, pageable)
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
