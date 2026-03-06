package com.bit.idol.userservice.dto.report;

import com.bit.idol.userservice.entity.Report;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportDto {
    private Long id;
    private int reporterId;
    private int targetUserId;
    private String reason;
    private String description;
    private LocalDateTime createdAt;

    public static ReportDto fromEntity(Report report) {
        return ReportDto.builder()
                .id(report.getId())
                .reporterId(report.getReporterId())
                .targetUserId(report.getTargetUserId())
                .reason(report.getReason())
                .description(report.getDescription())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
