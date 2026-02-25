package com.bit.subscriptionservice.repository;

import com.bit.subscriptionservice.entity.Subscription;
import com.bit.subscriptionservice.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {

    Optional<Subscription> findByUserIdAndIdolId(int userId, int idolId);

    List<Subscription> findAllByUserIdAndStatus(int userId, SubscriptionStatus status);

    boolean existsByUserIdAndIdolIdAndStatus(
            int userId,
            int idolId,
            SubscriptionStatus status
    );

    List<Subscription> findAllByStatusAndExpiredAtBefore(
            SubscriptionStatus status,
            LocalDateTime now
    );

    @Query("""
        SELECT s.userId
        FROM Subscription s
        WHERE s.idolId = :idolId
          AND s.status = :status
    """)
    List<Integer> selectUserIdsByIdolIdAndStatus(
            @Param("idolId") int idolId,
            @Param("status") SubscriptionStatus status
    );

    Optional<Subscription> findByUserIdAndIdolIdAndStatus(
            int userId,
            int idolId,
            SubscriptionStatus subscriptionStatus
    );

    // ✅ A안 핵심: 결제완료 이벤트에서 subscriptionId(targetId)로 조회할 때 사용
    Optional<Subscription> findByIdAndUserIdAndStatus(
            int id,
            int userId,
            SubscriptionStatus status
    );

    int countByIdolIdAndStatus(int idolId, SubscriptionStatus subscriptionStatus);
}
