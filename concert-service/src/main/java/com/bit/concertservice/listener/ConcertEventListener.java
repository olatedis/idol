package com.bit.concertservice.listener;

import com.bit.concertservice.domain.dto.NotificationEventDto;
import com.bit.concertservice.domain.entity.Concert;
import com.bit.concertservice.domain.event.ConcertEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConcertEventListener {

    private static final String NOTIFY_TOPIC = "notify-request-topic";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleConcertEvent(ConcertEvent event) {
        log.info("콘서트 알림 이벤트 처리 (After Commit): type={}, concertId={}", event.type(), event.concert().getId());
        publishConcertNotify(event.type(), event.concert());
    }

    private void publishConcertNotify(String type, Concert concert) {
        try {
            String eventId = UUID.randomUUID().toString();

            Map<String, String> args = new HashMap<>();
            args.put("concertId", String.valueOf(concert.getId()));
            args.put("concertName", concert.getTitle());
            args.put("openAt", concert.getConcertDate().toString());

            NotificationEventDto payload = NotificationEventDto.builder()
                    .eventId(eventId)
                    .type(type)
                    .targetType(NotificationEventDto.TargetType.GROUP_SUB)
                    .targetId(String.valueOf(concert.getGroupId()))
                    .args(args)
                    .redirectUrl("/concert/" + concert.getId())
                    .occurredAt(LocalDateTime.now())
                    .build();

            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(NOTIFY_TOPIC, json);

            log.info("콘서트 알림 발행 성공: type={}, groupId={}, concertId={}",
                    type, concert.getGroupId(), concert.getId());
        } catch (Exception e) {
            log.error("콘서트 알림 발행 실패: type={}, concertId={}, err={}", type, concert.getId(), e.getMessage());
        }
    }
}
