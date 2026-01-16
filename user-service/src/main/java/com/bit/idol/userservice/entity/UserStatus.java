package com.bit.idol.userservice.entity;

public enum UserStatus {
    ACTIVE,     // 정상 활동
    SUSPENDED,  // 일시 정지 (조회만 가능, 투표/글쓰기 불가) - 신고 누적 시 자동 전환
    BANNED      // 영구 정지 (로그인 불가) - 관리자 처리
}
