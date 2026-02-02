package com.bit.idol.userservice.dto;

import com.bit.idol.userservice.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMyPageDto {
    private int id;
    private String username;
    private String nickname;
    private String email;
    private String profileImage;
    private Role role;
    
    // 타 서비스 데이터 (Aggregation)
    private int subscriptionCount; // 구독 중인 아이돌 수
}
