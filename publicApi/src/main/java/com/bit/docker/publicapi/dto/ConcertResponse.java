package com.bit.docker.publicapi.dto;


public record ConcertResponse(
        String id,
        String title,
        String startDate,
        String endDate,
        String place,
        String poster
) {}

