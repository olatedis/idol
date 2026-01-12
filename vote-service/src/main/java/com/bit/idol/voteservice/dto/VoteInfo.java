package com.bit.idol.voteservice.dto;

import com.bit.idol.voteservice.entity.Vote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// Serializable 는 자바 내부 객체를 외부 (Redis)로 보내주기 위해 사용하는 인터페이스
public class VoteInfo implements Serializable {
    private int id;
    private String title;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public static VoteInfo from(Vote vote) {
        return VoteInfo.builder()
                .id(vote.getId())
                .title(vote.getTitle())
                .startDate(vote.getStartDate())
                .endDate(vote.getEndDate())
                .build();
    }
}
