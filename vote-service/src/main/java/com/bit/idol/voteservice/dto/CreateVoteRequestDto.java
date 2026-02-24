package com.bit.idol.voteservice.dto;

import com.bit.idol.voteservice.entity.Candidate;
import com.bit.idol.voteservice.entity.Vote;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    @Valid
    @NotNull(message = "후보 목록은 필수입니다.")
    @Size(min = 2, message = "후보는 최소 2명 이상이어야 합니다.")
    private List<CandidateDto> candidates; // 후보 목록 추가

    private Long targetGroupId; // 특정 그룹 대상 투표 (null이면 전체)

    public Vote toEntity() {
        Vote vote = new Vote();
        vote.setTitle(this.title);
        vote.setDescription(this.description);
        vote.setStartDate(this.startDate);
        vote.setEndDate(this.endDate);
        vote.setTargetGroupId(this.targetGroupId);

        if (candidates != null) {
            List<Candidate> candidateEntities = candidates.stream()
                    .map(dto -> {
                        Candidate candidate = new Candidate();
                        candidate.setNumber(dto.getNumber());
                        candidate.setName(dto.getName());
                        candidate.setImage(dto.getImage());
                        candidate.setVote(vote); // 연관관계 설정 필수
                        return candidate;
                    })
                    .collect(Collectors.toList());
            vote.setCandidate(candidateEntities);
        }

        return vote;
    }
}
