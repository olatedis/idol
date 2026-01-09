package com.bit.idol.voteservice.service;

import com.bit.idol.voteservice.entity.Candidate;
import com.bit.idol.voteservice.entity.VoteRecord;
import com.bit.idol.voteservice.repository.CandidateRepository;
import com.bit.idol.voteservice.repository.VoteRecordRepository;
import com.bit.idol.voteservice.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteConsumer {
    private final VoteRepository voteRepository;
    private final CandidateRepository candidateRepository;
    private final VoteRecordRepository voteRecordRepository;

    @Transactional
    @KafkaListener(topics = "vote-topic", groupId = "vote-group")
    public void consume(String message) {

        log.info("받은 메세지: {}", message);

        // 메세지 파싱

        String[] parts = message.split(":");

        int voteId = Integer.parseInt(parts[0]);
        int userId = Integer.parseInt(parts[1]);
        int candidateNumber = Integer.parseInt(parts[2]);

        // DB 저장
        Candidate candidate = candidateRepository.findByVoteIdAndCandidateNumber(voteId,candidateNumber)
                .orElseThrow(()-> new RuntimeException("해당 투표에 존재하지 않는 투표 번호입니다."));

        candidate.setVoteCount(candidate.getVoteCount() + 1);

        // 투표 이력 저장
        VoteRecord record = new VoteRecord();
        record.setVoteId(voteId);
        record.setUserId(userId);
        record.setCandidateId(candidate.getId());

        voteRecordRepository.save(record);

    }

}
