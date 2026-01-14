package com.bit.docker.paymentservice.api;


import com.bit.docker.paymentservice.domain.entity.Payment;
import com.bit.docker.paymentservice.infra.persistence.PaymentRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/{reservationId}")
    public Payment getPayment(@PathVariable Long reservationId) {
        return paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보가 없습니다."));
    }
}
