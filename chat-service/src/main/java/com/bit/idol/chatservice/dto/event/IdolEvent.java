package com.bit.idol.chatservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdolEvent {
    private String type; // CREATE, UPDATE
    private int idolId;
    private String stageName;
    private String profileImage;
    private String status;
}
