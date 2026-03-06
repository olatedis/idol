package com.bit.idol.userservice.dto.report;

import com.bit.idol.userservice.entity.UserStatus;
import lombok.Data;

@Data
public class UserStatusChangeDto {
    private int targetUserId;
    private UserStatus newStatus; // BANNED, ACTIVE, SUSPENDED, RESTRICTED
    private String reason;
    private Integer durationDays; // 정지일수 (1, 3, 7, 30 등), null이면 무기한
}
