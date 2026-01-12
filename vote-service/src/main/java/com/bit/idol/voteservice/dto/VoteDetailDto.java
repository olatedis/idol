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
    private List<CandidateDto> candidates;

    public static VoteDetailDto from(Vote vote) {
        return VoteDetailDto.builder()
                .id(vote.getId())
                .title(vote.getTitle())
                .description(vote.getDescription())
                .startDate(vote.getStartDate())
                .endDate(vote.getEndDate())
                .candidates(vote.getCandidate().stream()
                        .map(CandidateDto::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
