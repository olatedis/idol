package com.bit.idol.boardservice.dto.event;

public enum TargetType {
    USER,        // 특정 유저 1명
    IDOL_SUB,   // 특정 아이돌 구독자 전체
    GROUP_SUB,  // 특정 그룹 구독자 전체
    ALL         // 시스템 전체 유저
}
