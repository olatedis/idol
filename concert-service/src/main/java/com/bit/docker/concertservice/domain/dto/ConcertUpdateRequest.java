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
public class ConcertUpdateRequest {
    private String title;
    private String description;
    private String venue;
    private LocalDateTime concertDate;
    private LocalDateTime startTime;
    private LocalDateTime ticketSaleDate;
}
