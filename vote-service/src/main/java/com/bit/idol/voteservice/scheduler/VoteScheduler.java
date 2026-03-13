package com.bit.idol.voteservice.scheduler;

import com.bit.idol.voteservice.dto.notification.NotificationEventDto;
import com.bit.idol.voteservice.dto.notification.TargetType;
import com.bit.idol.voteservice.entity.Vote;
import com.bit.idol.voteservice.entity.VoteStatus;
import com.bit.idol.voteservice.producer.NotificationProducer;
import com.bit.idol.voteservice.repository.VoteRepository;
import com.bit.idol.voteservice.service.VoteService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    private final RedisTemplate<String, String> redisTemplate;
    private final NotificationProducer notificationProducer;
    private final VoteService voteService;

    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "closeExpiredVotes", lockAtLeastFor = "PT30S", lockAtMostFor = "PT50S")
    public void closeExpiredVotes() {
        LocalDateTime now = LocalDateTime.now();
        List<Vote> expiredVotes = voteRepository.findAllByEndDateBeforeAndStatus(now, VoteStatus.OPEN);

        if (!expiredVotes.isEmpty()) {
            log.info("마감된 투표 {}건을 종료 처리합니다.", expiredVotes.size());

            for (Vote vote : expiredVotes) {
                try {
                    voteService.closeVote(vote.getId());
                } catch (Exception e) {
                    log.error("투표 종료 처리 실패: ID={}, Error={}", vote.getId(), e.getMessage(), e);
                }
            }
        }
    }

    @Scheduled(cron = "0 0/10 * * * *")
    @SchedulerLock(name = "notifyClosingVotes", lockAtLeastFor = "PT30S", lockAtMostFor = "PT50S")
    public void notifyClosingVotes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);
        LocalDateTime oneHourTenMinutesLater = now.plusHours(1).plusMinutes(10);

        List<Vote> closingVotes = voteRepository.findAllByEndDateBetweenAndStatus(
                oneHourLater,
                oneHourTenMinutesLater,
                VoteStatus.OPEN
        );

        for (Vote vote : closingVotes) {
            String notifyKey = "vote:notify:closing:" + vote.getId();

            if (Boolean.FALSE.equals(redisTemplate.hasKey(notifyKey))) {
                TargetType targetType = TargetType.ALL;
                String targetId = null;

                if (vote.getTargetGroupId() != null) {
                    targetType = TargetType.GROUP_SUB;
                    targetId = String.valueOf(vote.getTargetGroupId());
                }

                sendVoteNotification(vote, "VOTE_CLOSING_SOON", targetType, targetId);
                redisTemplate.opsForValue().set(notifyKey, "SENT", java.time.Duration.ofHours(2));
            }
        }
    }

    private void sendVoteNotification(Vote vote, String type, TargetType targetType, String targetId) {
        try {
            Map<String, String> args = new HashMap<>();

            // 수정: 투표 제목 / groupId 전달 및 프론트 라우트에 맞는 redirectUrl 생성
            String redirectUrl = "/vote";

            if (vote != null) {
                args.put("voteTitle", vote.getTitle());

                if (vote.getTargetGroupId() != null) {
                    args.put("groupId", String.valueOf(vote.getTargetGroupId()));
                    redirectUrl = "/group/" + vote.getTargetGroupId() + "/vote";
                } else {
                    redirectUrl = "/vote";
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