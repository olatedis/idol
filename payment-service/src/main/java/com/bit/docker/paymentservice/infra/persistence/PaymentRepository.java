package com.bit.docker.paymentservice.infra.persistence;


import com.bit.docker.paymentservice.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReservationId(Long reservationId);
}
