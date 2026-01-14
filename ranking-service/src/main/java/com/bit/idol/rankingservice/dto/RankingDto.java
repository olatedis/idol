package com.bit.idol.rankingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankingDto {
    private int candidateNumber;
    private int score;
    private int delta; // 변동폭 (예: +5, 0)
}
