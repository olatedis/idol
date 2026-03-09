package com.bit.paymentservice.domain.dto;

import com.bit.paymentservice.domain.enumtype.PaymentDomain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter // Setter 추가
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateRequest {
    private int userId;
    private int amount;
    private PaymentDomain domain; // 결제 요청 서비스
    private int targetId; // 결제 상품 아이디
    private int agencyId; // 결제 대상 소속사 아이디 (매출 집계용)
    private List<Integer> reservationIds; // 예약 ID 목록 (콘서트 결제 시 필수)
}
