package com.bit.docker.subscriptionservice.repository;

import com.bit.docker.subscriptionservice.entity.GroupSubscription;
import com.bit.docker.subscriptionservice.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 0121 그룹id관련 생성
public interface GroupSubscriptionRepository extends JpaRepository<GroupSubscription, Integer> {

    Optional<GroupSubscription> findByUserIdAndGroupId(int userId, int groupId);

    List<GroupSubscription> findAllByUserIdAndStatus(int userId, SubscriptionStatus status);

    boolean existsByUserIdAndGroupIdAndStatus(int userId, int groupId, SubscriptionStatus status);

    List<GroupSubscription> findAllByStatusAndExpiredAtBefore(SubscriptionStatus status, LocalDateTime now);

    @Query("""
        SELECT gs.userId
        FROM GroupSubscription gs
        WHERE gs.groupId = :groupId
          AND gs.status = :status
    """)
    List<Integer> selectUserIdsByGroupIdAndStatus(@Param("groupId") int groupId, @Param("status") SubscriptionStatus status);
}
