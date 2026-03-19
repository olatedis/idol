package com.bit.idol.userservice.entity;

public enum UserStatus {
    ACTIVE, // 정상 활동
    SUSPENDED, // 일시 정지 (로그인은 되나 모든 활동 불가) - 관리자 처리
    RESTRICTED, // 경고/제한 (조회만 가능, 쓰기/채팅 발송 불가) - 신고 누적 시 자동 전환
    BANNED, // 영구 정지 (로그인 불가) - 관리자 처리
    WITHDRAWN // 회원 탈퇴
}
