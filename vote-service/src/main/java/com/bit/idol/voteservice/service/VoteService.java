package com.bit.idol.voteservice.service;

import com.bit.idol.voteservice.entity.Candidate;
import com.bit.idol.voteservice.repository.CandidateRepository;
import com.bit.idol.voteservice.repository.VoteRecordRepository;
import com.bit.idol.voteservice.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // 투표
    @Transactional
    public String castVote (int voteId, int userId, int candidateNumber) {

        // redis 키 생성
        String redisKey = "vote:" + voteId + ":user:" + userId;

        // redis를 통한 중복 체크
        // setIfAbsent 는 값이 없으면 저장 후 true 반환, 이미 있으면 false 반환

        Boolean isVoted = redisTemplate.opsForValue().setIfAbsent(redisKey, "voted", Duration.ofDays(7));

        if (Boolean.FALSE.equals(isVoted)) {
            throw new  RuntimeException("이미 투표에 참여하였습니다.");
        }

        // 카프카로 메세지 전송

        String message = voteId + ":" + userId + ":" + candidateNumber;
        kafkaTemplate.send("vote-topic", message);

        return "투표가 완료되었습니다.";
    }
}
