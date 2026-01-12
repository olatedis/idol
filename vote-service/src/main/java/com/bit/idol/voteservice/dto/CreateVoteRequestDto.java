package com.bit.idol.voteservice.dto;

import com.bit.idol.voteservice.entity.Vote;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateVoteRequestDto {

    @NotBlank(message = "투표 제목은 필수입니다.")
    @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 입력해주세요.")
    private String title;

    private String description;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDateTime startDate;

    @NotNull(message = "종료일은 필수입니다.")
    @Future(message = "종료일은 현재 시간보다 미래여야 합니다.")
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
