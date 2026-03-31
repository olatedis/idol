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

    List<Payment> findByUserIdAndTargetIdAndDomainAndStatus(int userId, int targetId, com.bit.paymentservice.domain.enumtype.PaymentDomain domain, com.bit.paymentservice.domain.enumtype.PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.agencyId = :agencyId AND p.status = :status")
    long sumAmountByAgencyIdAndStatusCompleted(@Param("agencyId") int agencyId, @Param("status") com.bit.paymentservice.domain.enumtype.PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.agencyId = :agencyId AND p.status = :status AND p.domain = :domain")
    long sumAmountByAgencyIdAndStatusCompletedAndDomain(
            @Param("agencyId") int agencyId,
            @Param("status") com.bit.paymentservice.domain.enumtype.PaymentStatus status,
            @Param("domain") PaymentDomain domain);
}
