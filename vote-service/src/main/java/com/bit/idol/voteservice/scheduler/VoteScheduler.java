package com.bit.idol.voteservice.scheduler;

import com.bit.idol.voteservice.entity.Vote;
import com.bit.idol.voteservice.entity.VoteStatus;
import com.bit.idol.voteservice.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteScheduler {

    private final VoteRepository voteRepository;

    // 1분마다 실행 (초 분 시 일 월 요일)
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void closeExpiredVotes() {
        LocalDateTime now = LocalDateTime.now();

        // 마감 시간이 지났는데 아직 OPEN인 투표들 조회
        List<Vote> expiredVotes = voteRepository.findAllByEndDateBeforeAndStatus(now, VoteStatus.OPEN);

        if (!expiredVotes.isEmpty()) {
            log.info("마감된 투표 {}건을 종료 처리합니다.", expiredVotes.size());
            
            for (Vote vote : expiredVotes) {
                vote.setStatus(VoteStatus.CLOSED);
                log.info("투표 종료 처리 완료: ID={}, 제목={}", vote.getId(), vote.getTitle());
                
                // 추후 Kafka 이벤트 발행 등 후처리 로직 추가 가능
            }
        }
    }
}
