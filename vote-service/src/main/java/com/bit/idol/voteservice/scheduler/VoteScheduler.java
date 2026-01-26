package com.bit.idol.voteservice.scheduler;

import com.bit.idol.voteservice.dto.notification.NotificationEventDto;
import com.bit.idol.voteservice.dto.notification.TargetType;
import com.bit.idol.voteservice.entity.Vote;
import com.bit.idol.voteservice.entity.VoteStatus;
import com.bit.idol.voteservice.producer.NotificationProducer;
import com.bit.idol.voteservice.repository.VoteRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteScheduler {

    private final VoteRepository voteRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationProducer notificationProducer;

    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "closeExpiredVotes", lockAtLeastFor = "PT30S", lockAtMostFor = "PT50S")
    @Transactional
    public void closeExpiredVotes() {
        LocalDateTime now = LocalDateTime.now();
        List<Vote> expiredVotes = voteRepository.findAllByEndDateBeforeAndStatus(now, VoteStatus.OPEN);

        if (!expiredVotes.isEmpty()) {
            log.info("마감된 투표 {}건을 종료 처리합니다.", expiredVotes.size());
            
            for (Vote vote : expiredVotes) {
                vote.setStatus(VoteStatus.CLOSED);
                
                String rankingKey = "vote:ranking:" + vote.getId();
                redisTemplate.delete(rankingKey);
                
                TargetType targetType = TargetType.ALL;
                String targetId = null;
                if (vote.getTargetGroupId() != null) {
                    targetType = TargetType.GROUP_SUB;
                    targetId = String.valueOf(vote.getTargetGroupId());
                }

                // 메시지 내용 제거
                sendVoteNotification(vote, "VOTE_CLOSED", targetType, targetId);

                log.info("투표 종료 및 Redis 정리 완료: ID={}, 제목={}", vote.getId(), vote.getTitle());
            }
        }
    }

    @Scheduled(cron = "0 0/10 * * * *")
    @SchedulerLock(name = "notifyClosingVotes", lockAtLeastFor = "PT30S", lockAtMostFor = "PT50S")
    public void notifyClosingVotes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);
        LocalDateTime oneHourTenMinutesLater = now.plusHours(1).plusMinutes(10);

        List<Vote> closingVotes = voteRepository.findAllByEndDateBetweenAndStatus(oneHourLater, oneHourTenMinutesLater, VoteStatus.OPEN);

        for (Vote vote : closingVotes) {
            String notifyKey = "vote:notify:closing:" + vote.getId();
            if (!redisTemplate.hasKey(notifyKey)) {
                
                TargetType targetType = TargetType.ALL;
                String targetId = null;
                if (vote.getTargetGroupId() != null) {
                    targetType = TargetType.GROUP_SUB;
                    targetId = String.valueOf(vote.getTargetGroupId());
                }

                // 메시지 내용 제거
                sendVoteNotification(vote, "VOTE_CLOSING_SOON", targetType, targetId);
                
                redisTemplate.opsForValue().set(notifyKey, "SENT", java.time.Duration.ofHours(2));
            }
        }
    }

    private void sendVoteNotification(Vote vote, String type, TargetType targetType, String targetId) {
        try {
            Map<String, String> args = new HashMap<>();
            
            String redirectUrl = "/vote";
            if (vote != null) {
                args.put("voteTitle", vote.getTitle());
                redirectUrl = "/vote/" + vote.getId();
            }

            NotificationEventDto event = NotificationEventDto.builder()
                    .eventId(UUID.randomUUID().toString())
                    .type(type)
                    .targetType(targetType)
                    .targetId(targetId)
                    .args(args) // 메시지 없이 변수만 전달
                    .redirectUrl(redirectUrl)
                    .occurredAt(LocalDateTime.now())
                    .build();
            
            notificationProducer.send(event);
        } catch (Exception e) {
            log.error("알림 발송 실패: {}", e.getMessage());
        }
    }
}
