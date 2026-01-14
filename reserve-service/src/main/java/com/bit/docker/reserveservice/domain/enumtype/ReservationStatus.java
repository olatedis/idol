package com.bit.docker.reserveservice.domain.enumtype;


public enum ReservationStatus {
    PENDING,    // 선점 완료, 결제 대기
    CONFIRMED,  // 결제 완료
    CANCELED    // 취소 / 만료
}
