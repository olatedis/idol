package com.bit.idol.userservice.dto.user;

import com.bit.idol.userservice.entity.BanHistory;
import com.bit.idol.userservice.entity.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BanHistoryDto {
    private Long id;
    private int userId;
    private UserStatus status;
    private String reason;
    private LocalDateTime createdAt;

    public static BanHistoryDto fromEntity(BanHistory history) {
        return BanHistoryDto.builder()
                .id(history.getId())
                .userId(history.getUserId())
                .status(history.getStatus())
                .reason(history.getReason())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
