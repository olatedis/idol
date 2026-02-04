package com.bit.subscriptionservice.repository;

import com.bit.subscriptionservice.entity.BillingKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BillingKeyRepository extends JpaRepository<BillingKey, Integer> {
    Optional<BillingKey> findByCustomerKey(String customerKey);
    Optional<BillingKey> findByBillingKey(String billingKey);
    Optional<BillingKey> findByUserIdAndIdolIdAndActiveTrue(int userId, int idolId);
}
