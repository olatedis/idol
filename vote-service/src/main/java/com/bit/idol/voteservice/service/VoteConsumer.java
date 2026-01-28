package com.bit.idol.voteservice.service;

import com.bit.idol.voteservice.dto.NotificationEventDto;
import com.bit.idol.voteservice.entity.Candidate;
import com.bit.idol.voteservice.entity.VoteRecord;
import com.bit.idol.voteservice.repository.CandidateRepository;
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

import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteConsumer {
    private final VoteRepository voteRepository;
    private final CandidateRepository candidateRepository;
    private final VoteRecordRepository voteRecordRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final VoteReader voteReader; // 추가됨

    @Transactional
    @KafkaListener(topics = "vote-topic", groupId = "vote-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(List<String> messages) {

        log.info("배치 처리 시작: {}건", messages.size());

        List<VoteRecord> recordsToSave = new ArrayList<>();
        List<String> validMessages = new ArrayList<>();
        
        // 1. 로컬 캐시 (배치 내 중복 조회 방지)
        Map<String, Candidate> candidateCache = new HashMap<>();
        // 2. 득표수 집계 (후보자 ID -> 증가할 표 수)
        Map<Integer, Integer> voteCountMap = new HashMap<>();

        for (String message : messages) {
            try {
                String[] parts = message.split(":");
                if (parts.length < 4) continue;

                String uuid = parts[0];
                int voteId = Integer.parseInt(parts[1]);
                int userId = Integer.parseInt(parts[2]);
                int candidateNumber = Integer.parseInt(parts[3]);

                String processedKey = "processed:vote-msg:" + uuid;
                Boolean isNew = redisTemplate.opsForValue().setIfAbsent(processedKey, "1", Duration.ofMinutes(10));

                if (Boolean.FALSE.equals(isNew)) continue;

                // 3. 후보자 조회 (Redis 캐싱 + 로컬 캐싱)
                String cacheKey = voteId + ":" + candidateNumber;
                Candidate candidate = candidateCache.computeIfAbsent(cacheKey, k -> 
                        voteReader.getCandidate(voteId, candidateNumber) // VoteReader 사용
                );

                // 4. 득표수 집계 (메모리 합산)
                voteCountMap.merge(candidate.getId(), 1, Integer::sum);

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

        // 5. JDBC Batch Insert (투표 이력)
        if (!recordsToSave.isEmpty()) {
            String sql = "INSERT INTO vote_record (vote_id, user_id, candidate_id, voted_at) VALUES (?, ?, ?, ?)";
            
            jdbcTemplate.batchUpdate(sql, recordsToSave, recordsToSave.size(),
                    (PreparedStatement ps, VoteRecord record) -> {
                        ps.setInt(1, record.getVoteId());
                        ps.setInt(2, record.getUserId());
                        ps.setInt(3, record.getCandidateId());
                        ps.setObject(4, record.getVotedAt());
                    });
            
            log.info("DB Batch Insert 완료: {}건", recordsToSave.size());
        }

        // 6. JDBC Batch Update (득표수 증가)
        if (!voteCountMap.isEmpty()) {
            String updateSql = "UPDATE candidate SET vote_count = vote_count + ? WHERE id = ?";
            List<Map.Entry<Integer, Integer>> updates = new ArrayList<>(voteCountMap.entrySet());
            
            jdbcTemplate.batchUpdate(updateSql, updates, updates.size(),
                    (PreparedStatement ps, Map.Entry<Integer, Integer> entry) -> {
                        ps.setInt(1, entry.getValue()); // 증가할 표 수
                        ps.setInt(2, entry.getKey());   // 후보자 ID
                    });
            
            log.info("DB Batch Update 완료: 후보자 {}명 득표수 갱신", updates.size());
        }

        // 7. 후속 이벤트 발행
        for (String msg : validMessages) {
            kafkaTemplate.send("vote-complete-topic", msg);
        }
    }
}
