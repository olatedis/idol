package com.bit.idol.voteservice.dto;

import com.bit.idol.voteservice.entity.Vote;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateVoteRequestDto {
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public Vote toEntity() {
        Vote vote = new Vote();
        vote.setTitle(this.title);
        vote.setDescription(this.description);
        vote.setStartDate(this.startDate);
        vote.setEndDate(this.endDate);
        return vote;
    }
}
