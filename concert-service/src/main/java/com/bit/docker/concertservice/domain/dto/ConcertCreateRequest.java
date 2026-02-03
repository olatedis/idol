package com.bit.docker.concertservice.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConcertCreateRequest {
    private int agencyId;          // 소속사 ID
    private int groupId;            // 그룹 ID
    private String title;          // 콘서트 이름
    private String description;    // 설명
    private String venue;          // 장소
    private LocalDateTime concertDate;   // 시작일
    private LocalDateTime startTime;     // 시간
    private LocalDateTime ticketSaleDate; // 티켓 판매일
}
