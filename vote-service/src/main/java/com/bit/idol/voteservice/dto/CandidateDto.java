package com.bit.idol.voteservice.dto;

import com.bit.idol.voteservice.entity.Candidate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDto {
    private int id;
    private int candidateNumber;
    private String name;
    private Integer voteCount;

    public static CandidateDto from(Candidate candidate) {
        return CandidateDto.builder()
                .id(candidate.getId())
                .candidateNumber(candidate.getCandidateNumber())
                .name(candidate.getName())
                .voteCount(candidate.getVoteCount())
                .build();
    }
}
