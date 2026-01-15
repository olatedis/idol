package com.bit.docker.subscriptionservice.repository;

import com.bit.docker.subscriptionservice.entity.Subscription;
import com.bit.docker.subscriptionservice.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserIdAndIdolId(int userId, Long idolId);
    List<Subscription> findAllByUserIdAndStatus(int userId, SubscriptionStatus status);
    boolean existsByUserIdAndIdolIdAndStatus(
            int userId,
            Long idolId,
            SubscriptionStatus status
    );
    List<Subscription> findAllByStatusAndExpiredAtBefore(
            SubscriptionStatus status,
            LocalDateTime now
    );
}

