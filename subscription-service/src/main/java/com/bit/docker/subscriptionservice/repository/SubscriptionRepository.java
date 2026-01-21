package com.bit.docker.subscriptionservice.repository;

import com.bit.docker.subscriptionservice.entity.Subscription;
import com.bit.docker.subscriptionservice.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 0121 그룹id관련 수정(추가)
    @Query("""
        SELECT s.userId
        FROM Subscription s
        WHERE s.idolId = :idolId
          AND s.status = :status
    """)
    List<Integer> selectUserIdsByIdolIdAndStatus(@Param("idolId") Long idolId, @Param("status") SubscriptionStatus status);
}
