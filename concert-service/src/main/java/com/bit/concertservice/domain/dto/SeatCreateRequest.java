package com.bit.concertservice.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatCreateRequest {
    private String grade;  // "VIP", "R", "S", "A"
    private int count;
    private int price;
}