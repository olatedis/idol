package com.bit.concertservice.domain.dto;

import com.bit.concertservice.domain.entity.Concert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConcertResponse {
    private int id;
    private int agencyId;
    private String title;
    private String description;
    private String venue;
    private LocalDateTime concertDate;
    private LocalDateTime startTime;
    private LocalDateTime ticketSaleDate;
    private LocalDateTime createdAt;

    public static ConcertResponse from(Concert concert) {
        return ConcertResponse.builder()
                .id(concert.getId())
                .agencyId(concert.getAgencyId())
                .title(concert.getTitle())
                .description(concert.getDescription())
                .venue(concert.getVenue())
                .concertDate(concert.getConcertDate())
                .startTime(concert.getStartTime())
                .ticketSaleDate(concert.getTicketSaleDate())
                .createdAt(concert.getCreatedAt())
                .build();
    }
}
