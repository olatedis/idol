package com.bit.docker.openapiservice.dto;


public record ConcertResponse(
        String id,
        String title,
        String startDate,
        String endDate,
        String place,
        String poster
) {}

