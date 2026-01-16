package com.bit.idol.userservice.dto.report;

import lombok.Data;

@Data
public class ReportRequestDto {
    private int targetUserId;
    private String reason;
    private String description;
}
