package com.bit.idol.voteservice.dto;

import com.bit.idol.voteservice.entity.Candidate;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDto {
    private Integer id;
    
    @JsonProperty("number")
    private Integer number;
    
    private String name;
    private String image; // 이미지 필드 추가
    private Integer voteCount;

    public static CandidateDto from(Candidate candidate) {
        return CandidateDto.builder()
                .id(candidate.getId())
                .number(candidate.getNumber())
                .name(candidate.getName())
                .image(candidate.getImage())
                .voteCount(candidate.getVoteCount())
                .build();
    }
}
