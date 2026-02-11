package com.bit.reserveservice.listener;

import com.bit.reserveservice.domain.event.ReservationCreatedEvent;
import com.bit.reserveservice.infra.kafka.ReservationEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationEventListener {

    private final ReservationEventProducer eventProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationCreated(ReservationCreatedEvent eventWrapper) {
        log.info("예약 생성 이벤트 처리 (After Commit): userId={}, seatId={}", 
                eventWrapper.event().getUserId(), eventWrapper.event().getTargetId());
        
        try {
            eventProducer.publishPaymentRequested(eventWrapper.event());
        } catch (Exception e) {
            log.error("Kafka 결제 요청 발행 실패: {}", e.getMessage());
            // TODO: 여기서 실패하면 보상 트랜잭션(예약 취소)이 필요할 수 있음.
            // 하지만 일단 로그만 남기고, 스케줄러가 만료된 예약을 정리하도록 둠.
        }
    }
}
