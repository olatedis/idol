package com.bit.paymentservice.domain.dto;

import com.bit.paymentservice.domain.enumtype.PaymentDomain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter // Setter 추가
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateRequest {
    private int userId;
    private int amount;
    private PaymentDomain domain; // 결제 요청 서비스
    private int targetId; // 결제 상품 아이디
}
