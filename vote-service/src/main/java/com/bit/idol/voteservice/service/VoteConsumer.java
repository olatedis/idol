package com.bit.idol.voteservice.service;

import com.bit.idol.voteservice.entity.Candidate;
import com.bit.idol.voteservice.entity.VoteRecord;
import com.bit.idol.voteservice.repository.VoteRecordRepository;
import com.bit.idol.voteservice.repository.VoteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement; // import 추가
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteConsumer {
    private final VoteRepository voteRepository;
    private final VoteRecordRepository voteRecordRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final VoteReader voteReader;

    @Transactional
    @KafkaListener(topics = "${spring.kafka.topic.vote}", groupId = "vote-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(List<String> messages) {

        log.info("배치 처리 시작: {}건", messages.size());

        List<VoteRecord> recordsToSave = new ArrayList<>();
        List<String> validMessages = new ArrayList<>();

        Map<String, Candidate> candidateCache = new HashMap<>();
        Map<Integer, Integer> voteCountMap = new HashMap<>();
        Map<Integer, Integer> voteTotalMap = new HashMap<>();

        for (String message : messages) {
            try {
                String[] parts = message.split(":");
                if (parts.length < 4)
                    continue;

                int voteId = Integer.parseInt(parts[1]);
                int userId = Integer.parseInt(parts[2]);
                int candidateNumber = Integer.parseInt(parts[3]);

                String idempotencyKey = "processed:vote:" + voteId + ":user:" + userId;

                Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "1", Duration.ofMinutes(10));

                if (Boolean.FALSE.equals(isNew)) {
                    log.warn("중복 투표 메시지 무시: voteId={}, userId={}", voteId, userId);
                    continue;
                }

                String cacheKey = voteId + ":" + candidateNumber;
                Candidate candidate = candidateCache.computeIfAbsent(cacheKey,
                        k -> voteReader.getCandidate(voteId, candidateNumber));

                voteCountMap.merge(candidate.getId(), 1, Integer::sum);
                voteTotalMap.merge(voteId, 1, Integer::sum);

                VoteRecord record = new VoteRecord();
                record.setVoteId(voteId);
                record.setUserId(userId);
                record.setCandidateId(candidate.getId());
                record.setVotedAt(LocalDateTime.now());

                recordsToSave.add(record);
                validMessages.add(message);

            } catch (Exception e) {
                log.error("메시지 처리 실패: {}", message, e);
            }
        }

        if (!recordsToSave.isEmpty()) {
            voteRecordRepository.saveAll(recordsToSave);
            log.info("JPA saveAll Insert 완료: {}건", recordsToSave.size());
        }

        if (!voteCountMap.isEmpty()) {
            String updateSql = "UPDATE candidate SET vote_count = vote_count + ? WHERE id = ?";
            List<Map.Entry<Integer, Integer>> updates = new ArrayList<>(voteCountMap.entrySet());

            jdbcTemplate.batchUpdate(updateSql, updates, updates.size(),
                    (PreparedStatement ps, Map.Entry<Integer, Integer> entry) -> {
                        ps.setInt(1, entry.getValue());
                        ps.setInt(2, entry.getKey());
                    });

            log.info("DB Batch Update (Candidate) 완료: {}건", updates.size());
        }

        if (!voteTotalMap.isEmpty()) {
            String updateSql = "UPDATE vote SET total_votes = total_votes + ? WHERE id = ?";
            List<Map.Entry<Integer, Integer>> updates = new ArrayList<>(voteTotalMap.entrySet());

            jdbcTemplate.batchUpdate(updateSql, updates, updates.size(),
                    (PreparedStatement ps, Map.Entry<Integer, Integer> entry) -> {
                        ps.setInt(1, entry.getValue());
                        ps.setInt(2, entry.getKey());
                    });

            log.info("DB Batch Update (Vote Total) 완료: {}건", updates.size());
        }

        for (String msg : validMessages) {
            kafkaTemplate.send("vote-complete-topic", msg);
        }
    }
}
