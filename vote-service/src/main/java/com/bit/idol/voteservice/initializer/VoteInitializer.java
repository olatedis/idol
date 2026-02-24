package com.bit.idol.voteservice.initializer;

import com.bit.idol.voteservice.entity.Candidate;
import com.bit.idol.voteservice.entity.Vote;
import com.bit.idol.voteservice.entity.VoteStatus;
import com.bit.idol.voteservice.repository.CandidateRepository;
import com.bit.idol.voteservice.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteInitializer {

    private final VoteRepository voteRepository;
    private final CandidateRepository candidateRepository;
    private final RedisTemplate<String, String> redisTemplate; // <String, Object> -> <String, String>

    /**
     * 애플리케이션 시작 시 Redis 데이터 복구 (Warm-up)
     * DB에 있는 투표 현황을 Redis로 동기화
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void initRedisData() {
        log.info("Redis 데이터 워밍업 시작...");

        try {
            // 1. 진행 중인 투표 조회
            List<Vote> openVotes = voteRepository.findAllByStatus(VoteStatus.OPEN);

            if (openVotes.isEmpty()) {
                log.info("진행 중인 투표가 없습니다.");
                return;
            }

            for (Vote vote : openVotes) {
                String rankingKey = "vote:ranking:" + vote.getId();

                // 2. 후보자별 득표수 조회
                List<Candidate> candidates = candidateRepository.findAllByVoteId(vote.getId());

                for (Candidate candidate : candidates) {
                    // 3. Redis ZSET 복구 (이미 값이 있어도 덮어씀)
                    redisTemplate.opsForZSet().add(rankingKey, String.valueOf(candidate.getNumber()), candidate.getVoteCount());
                }

                // 4. 활성 목록 추가 (랭킹 서비스 스케줄러용)
                redisTemplate.opsForSet().add("vote:active-list", String.valueOf(vote.getId()));
                
                log.info("투표 복구 완료: ID={}, 후보자 수={}", vote.getId(), candidates.size());
            }

            log.info("Redis 데이터 워밍업 완료: 총 {}건의 투표 동기화됨", openVotes.size());

        } catch (Exception e) {
            log.error("Redis 워밍업 중 오류 발생", e);
        }
    }
}
