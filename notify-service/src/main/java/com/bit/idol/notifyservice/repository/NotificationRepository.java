package com.bit.idol.notifyservice.repository;

import com.bit.idol.notifyservice.entity.Notification;
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

    // 조회: receiverId 기준 최신순(커서 페이징)
    // cursor는 occurredAt 기준(이전 페이지 마지막 occurredAt을 넘겨받는 방식)
    @Query("""
        SELECT n
        FROM Notification n
        WHERE n.receiverId = :receiverId
          AND (:cursor IS NULL OR n.occurredAt < :cursor)
        ORDER BY n.occurredAt DESC
    """)
    List<Notification> findListByCursor(
            @Param("receiverId") int receiverId,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable
    );

    // 정리: 특정 유저 알림 일괄 삭제(예: USER_DELETED 대응)
    @Modifying
    @Query("""
        DELETE FROM Notification n
        WHERE n.receiverId = :receiverId
    """)
    int deleteAllByReceiverId(@Param("receiverId") int receiverId);
}
