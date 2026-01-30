package com.bit.idol.notifyservice.repository;

import com.bit.idol.notifyservice.entity.IdolMessageStack;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IdolMessageStackRepository extends JpaRepository<IdolMessageStack, Long> {

    Optional<IdolMessageStack> findByReceiverIdAndIdolId(int receiverId, long idolId);

    List<IdolMessageStack> findAllByReceiverIdOrderByLastOccurredAtDesc(int receiverId);

    /**
     * 스택 증가 UPSERT
     * - row 없으면 insert(unread=1)
     * - 있으면 unread_count + 1
     * - last_occurred_at은 더 최신이면 갱신
     *
     * MySQL 전용 문법: ON DUPLICATE KEY UPDATE
     */
    @Modifying
    @Query(value = """
        INSERT INTO idol_message_stack(receiver_id, idol_id, unread_count, last_occurred_at)
        VALUES (:receiverId, :idolId, 1, :occurredAt)
        ON DUPLICATE KEY UPDATE
            unread_count = unread_count + 1,
            last_occurred_at = CASE
                WHEN last_occurred_at < VALUES(last_occurred_at)
                THEN VALUES(last_occurred_at)
                ELSE last_occurred_at
            END
        """, nativeQuery = true)
    int upsertIncrease(@Param("receiverId") int receiverId,
                       @Param("idolId") long idolId,
                       @Param("occurredAt") LocalDateTime occurredAt);

    /**
     * 특정 idolId 스택 reset
     * - last_occurred_at은 유지
     */
    @Modifying
    @Query(value = """
        UPDATE idol_message_stack
        SET unread_count = 0
        WHERE receiver_id = :receiverId
          AND idol_id = :idolId
        """, nativeQuery = true)
    int resetUnread(@Param("receiverId") int receiverId,
                    @Param("idolId") long idolId);
}
