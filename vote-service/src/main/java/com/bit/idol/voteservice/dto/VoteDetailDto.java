package com.bit.idol.voteservice.dto;

import com.bit.idol.voteservice.entity.Vote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteDetailDto {
    private int id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private int totalVotes; // 총 투표수
    private List<CandidateDto> candidates;
    
    // 내가 투표한 후보 ID (로그인 유저용) - 추가됨
    private Integer myVotedCandidateId;

    public static VoteDetailDto from(Vote vote) {
        return VoteDetailDto.builder()
                .id(vote.getId())
                .title(vote.getTitle())
                .description(vote.getDescription())
                .startDate(vote.getStartDate())
                .endDate(vote.getEndDate())
                .status(vote.getStatus().name())
                .totalVotes(vote.getTotalVotes())
                .candidates(vote.getCandidate().stream()
                        .map(CandidateDto::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
