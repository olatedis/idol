package com.bit.idol.chatservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEventDto {
    private int userId;
    private String type; // CREATE, UPDATE, DELETE
    private String status; // ACTIVE, RESTRICTED, DELETED 등
}
