package com.bit.idol.userservice.dto.report;

import com.bit.idol.userservice.entity.UserStatus;
import lombok.Data;

@Data
public class UserStatusChangeDto {
    private int targetUserId;
    private UserStatus newStatus; // BANNED, ACTIVE, SUSPENDED
    private String reason;
}
