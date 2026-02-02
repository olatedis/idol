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
public class VoteListDto {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status; // PROGRESS, ENDED
    private int participantCount; // 참여자 수
    private boolean isVoted; // 내가 투표했는지 여부
    private String thumbnailUrl; // 투표 썸네일 (있다면)
}
