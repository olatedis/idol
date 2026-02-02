package com.bit.docker.reserveservice.domain.dto;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationEvent {
    private String eventId;      // UUID
    private String type;         // 알림 종류
    private TargetType targetType; // 대상 타입
    private String targetId;     // 대상 ID
    private Map<String, String> args; // 치환 변수
    private String redirectUrl;  // 클릭 시 이동할 주소
    private LocalDateTime occurredAt; // 발생 시간

    public enum TargetType {
        USER,       // 특정 유저 1명
        ALL,        // 전체 공지
        IDOL_SUB,   // 특정 아이돌 구독자들
        GROUP_SUB   // 특정 그룹 구독자들
    }

    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
