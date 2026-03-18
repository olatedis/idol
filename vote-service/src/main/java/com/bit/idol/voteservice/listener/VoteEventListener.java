package com.bit.idol.voteservice.listener;

import com.bit.idol.voteservice.dto.event.VoteEvent;
import com.bit.idol.voteservice.dto.notification.NotificationEventDto;
import com.bit.idol.voteservice.entity.Vote;
import com.bit.idol.voteservice.producer.NotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class VoteEventListener {

    private final NotificationProducer notificationProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVoteEvent(VoteEvent event) {
        log.info("투표 알림 이벤트 처리 (After Commit): type={}, voteId={}", event.type(), event.vote().getId());
        sendVoteNotification(event.vote(), event.type(), event.targetType(), event.targetId());
    }

    private void sendVoteNotification(
            Vote vote,
            String type,
            com.bit.idol.voteservice.dto.notification.TargetType targetType,
            String targetId
    ) {
        try {
            Map<String, String> args = new HashMap<>();

            // 수정: 투표 알림 문구용 제목 전달
            String redirectUrl = (vote != null && vote.getTargetGroupId() != null)
                    ? "/group/" + vote.getTargetGroupId() + "/vote"
                    : "/idol"; // 그룹 아이디가 없는 극히 예외적인 경우 아이돌 리스트로 리다이렉트

            if (vote != null) {
                args.put("voteTitle", vote.getTitle());

                if (vote.getTargetGroupId() != null) {
                    args.put("groupId", String.valueOf(vote.getTargetGroupId()));
                }
            }

            NotificationEventDto event = NotificationEventDto.builder()
                    .eventId(UUID.randomUUID().toString())
                    .type(type)
                    .targetType(targetType)
                    .targetId(targetId)
                    .args(args)
                    .redirectUrl(redirectUrl)
                    .occurredAt(LocalDateTime.now())
                    .build();

            notificationProducer.send(event);
        } catch (Exception e) {
            log.error("알림 발송 실패: {}", e.getMessage(), e);
        }
    }
}