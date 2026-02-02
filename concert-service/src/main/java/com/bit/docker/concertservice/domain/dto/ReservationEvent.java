package com.bit.docker.concertservice.domain.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationEvent {
    private String eventType; // CREATED, CANCELED, EXPIRED
    private TargetType targetType;
    private int userId;
    private int concertId;
    private int seatId;
    private LocalDateTime occurredAt;
    private int groupId;

    public enum TargetType { USER }

    public static ReservationEvent fromJson(String s) {
        try {
            return new ObjectMapper().readValue(s, ReservationEvent.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
