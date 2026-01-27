package com.bit.docker.reserveservice.domain.dto;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.tool.schema.TargetType;

import java.time.LocalDateTime;
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationEvent {
    private String eventType; // CREATED, CANCELED, EXPIRED
    private TargetType targetType= TargetType.USER;
    private int userId;
    private int concertId;
    private LocalDateTime occurredAt;

    // 0121 그룹id관련 수정(추가)
    private int groupId;

    public enum TargetType {
        USER
    }

    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
