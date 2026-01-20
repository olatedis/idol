package com.bit.docker.paymentservice.api;


import com.bit.docker.paymentservice.application.PaymentService;
import com.bit.docker.paymentservice.domain.dto.TossConfirmRequest;
import com.bit.docker.paymentservice.domain.entity.Payment;
import com.bit.docker.paymentservice.infra.persistence.PaymentRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@AllArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@RequestBody TossConfirmRequest dto) {
        paymentService.confirmPayment(
                dto.getPaymentKey(),
                dto.getOrderId(),
                dto.getAmount()
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{orderId}")
    public Payment getPayment(@PathVariable String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보가 없습니다."));
    }
}
