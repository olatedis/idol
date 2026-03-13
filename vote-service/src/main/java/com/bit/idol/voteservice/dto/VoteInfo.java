package com.bit.idol.voteservice.dto;

import com.bit.idol.voteservice.entity.Vote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteInfo implements Serializable {
    private Integer id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private Long targetGroupId; // 그룹 식별자 추가
    private List<CandidateDto> candidates; // 후보 목록 추가

    public static VoteInfo from(Vote vote) {
        LocalDateTime now = LocalDateTime.now();
        String status = "OPEN";

        if (vote.getStartDate() != null && now.isBefore(vote.getStartDate())) {
            status = "UPCOMING";
        } else if (vote.getEndDate() != null && now.isAfter(vote.getEndDate())) {
            status = "CLOSED";
        }

        return VoteInfo.builder()
                .id(vote.getId())
                .title(vote.getTitle())
                .description(vote.getDescription())
                .startDate(vote.getStartDate())
                .endDate(vote.getEndDate())
                .status(status)
                .targetGroupId(vote.getTargetGroupId())
                .candidates(vote.getCandidate().stream()
                        .map(CandidateDto::from)
                        .collect(Collectors.toList())) // 후보 목록 매핑
                .build();
    }
}
