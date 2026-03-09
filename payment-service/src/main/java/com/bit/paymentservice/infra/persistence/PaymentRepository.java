package com.bit.paymentservice.infra.persistence;

import com.bit.paymentservice.domain.entity.Payment;
import com.bit.paymentservice.domain.enumtype.PaymentDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(int userId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.agencyId = :agencyId AND p.status = 'COMPLETED'")
    long sumAmountByAgencyIdAndStatusCompleted(@Param("agencyId") int agencyId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.agencyId = :agencyId AND p.status = 'COMPLETED' AND p.domain = :domain")
    long sumAmountByAgencyIdAndStatusCompletedAndDomain(
            @Param("agencyId") int agencyId,
            @Param("domain") PaymentDomain domain);
}
