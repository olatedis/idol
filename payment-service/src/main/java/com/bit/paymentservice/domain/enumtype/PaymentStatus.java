package com.bit.paymentservice.domain.enumtype;


public enum PaymentStatus {
    READY,          // 결제 생성됨 (아직 PG 미결제)
    IN_PROGRESS,    // PG 결제 진행 중
    COMPLETED,      // 결제 완료
    FAILED,         // 결제 실패
    CANCELED        // 사용자 취소
}
