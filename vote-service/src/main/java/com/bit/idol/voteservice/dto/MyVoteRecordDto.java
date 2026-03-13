package com.bit.idol.voteservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyVoteRecordDto {
    private Integer voteId;
    private String voteTitle;
    private Integer candidateNumber;
    private String candidateName;
    private LocalDateTime votedAt;
}
