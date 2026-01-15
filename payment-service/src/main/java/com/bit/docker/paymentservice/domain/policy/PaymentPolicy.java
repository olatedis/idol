package com.bit.docker.paymentservice.domain.policy;


import com.bit.docker.paymentservice.domain.entity.Payment;
import com.bit.docker.paymentservice.domain.enumtype.PaymentStatus;

public class PaymentPolicy {

    public static boolean validatePayable(Payment payment) {
        if (payment.getStatus() != PaymentStatus.REQUESTED) {
            throw new IllegalStateException("결제를 진행할 수 없는 상태입니다.");
        }
        return true;
    }
}
