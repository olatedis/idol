package com.bit.idol.notifyservice.repository;

import com.bit.idol.notifyservice.entity.Notification;
import com.bit.idol.notifyservice.entity.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query("""
        SELECT n
        FROM Notification n
        WHERE n.receiverId = :receiverId
          AND (:category IS NULL OR n.category = :category)
          AND (:unreadOnly = false OR n.readAt IS NULL)
          AND (:cursor IS NULL OR n.createdAt < :cursor)
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findListByCursor(
            @Param("receiverId") int receiverId,
            @Param("category") NotificationType category,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable
    );

    long countByReceiverIdAndReadAtIsNull(int receiverId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.receiverId = :receiverId AND n.readAt IS NULL")
    int markAllRead(@Param("receiverId") int receiverId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.receiverId = :receiverId")
    int deleteAllByReceiverId(@Param("receiverId") int receiverId);
}
