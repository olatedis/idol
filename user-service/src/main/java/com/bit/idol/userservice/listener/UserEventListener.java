package com.bit.idol.userservice.listener;

import com.bit.idol.userservice.dto.event.UserEvent;
import com.bit.idol.userservice.producer.UserSyncProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {

    private final UserSyncProducer userSyncProducer;

    // 트랜잭션이 성공적으로 커밋된 후에만 실행됨
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserEvent(UserEvent event) {
        log.info("UserEvent 처리 (After Commit): userId={}, type={}", event.userId(), event.type());
        try {
            userSyncProducer.send(event.userId(), event.type());
        } catch (Exception e) {
            log.error("Kafka 이벤트 발행 실패: {}", e.getMessage());
            // 여기서 실패하면 재시도 로직이 필요할 수 있음 (Outbox Pattern의 필요성)
        }
    }
}
