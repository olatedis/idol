package com.bit.idol.notifyservice.repository;

import com.bit.idol.notifyservice.entity.Notification;
import com.bit.idol.notifyservice.entity.TargetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    // 중복 방지
    boolean existsByEventId(String eventId);

    Optional<Notification> findByEventId(String eventId);

    // 조회: targetType/targetId 기준 최신순(커서 페이징)
    // - USER 타겟이면 targetType=USER, targetId=<userId>
    // - ALL 타겟이면 targetType=ALL, targetId=null (또는 고정 문자열)
    @Query("""
        SELECT n
        FROM Notification n
        WHERE n.targetType = :targetType
          AND (
               (:targetId IS NULL AND n.targetId IS NULL)
               OR (:targetId IS NOT NULL AND n.targetId = :targetId)
          )
          AND (:cursor IS NULL OR n.occurredAt < :cursor)
        ORDER BY n.occurredAt DESC
    """)
    List<Notification> findListByCursor(
            @Param("targetType") TargetType targetType,
            @Param("targetId") String targetId,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable
    );

    // 정리: 특정 USER 타겟 알림 일괄 삭제 (USER_DELETED 대응)
    @Modifying
    @Query("""
        DELETE FROM Notification n
        WHERE n.targetType = :targetType
          AND n.targetId = :targetId
    """)
    int deleteAllByTarget(@Param("targetType") TargetType targetType,
                          @Param("targetId") String targetId);
}
