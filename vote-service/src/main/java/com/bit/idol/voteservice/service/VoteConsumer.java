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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final ObjectMapper objectMapper; // JSON 변환용

    @Transactional
    @KafkaListener(topics = "vote-topic", groupId = "vote-group")
    public void consume(String message) {

        log.info("받은 메세지: {}", message);

        // 메세지 파싱
        String[] parts = message.split(":");

        int voteId = Integer.parseInt(parts[0]);
        int userId = Integer.parseInt(parts[1]);
        int candidateNumber = Integer.parseInt(parts[2]);

        // 후보자 조회 (ID를 알기 위해 필요)
        Candidate candidate = candidateRepository.findByVoteIdAndCandidateNumber(voteId, candidateNumber)
                .orElseThrow(() -> new RuntimeException("해당 투표에 존재하지 않는 투표 번호입니다."));

        // 투표 수 증가 (직접 Update 쿼리 사용으로 동시성 문제 해결)
        candidateRepository.incrementVoteCount(candidate.getId());

        // 투표 이력 저장
        VoteRecord record = new VoteRecord();
        record.setVoteId(voteId);
        record.setUserId(userId);
        record.setCandidateId(candidate.getId());

        voteRecordRepository.save(record);
        
        // 1. 투표 완료 이벤트 발행 (랭킹 서비스용)
        // 토픽: vote-complete-topic
        // 메시지: voteId:userId:candidateNumber (기존 포맷 재사용)
        try {
            kafkaTemplate.send("vote-complete-topic", message);
            log.info("랭킹 업데이트 이벤트 발행 성공: {}", message);
        } catch (Exception e) {
            log.error("랭킹 업데이트 이벤트 발행 실패", e);
        }

        // 2. 알림 이벤트 발행 (알림 서비스용)
        // 토픽: notification-topic
        // 메시지: JSON String (NotificationEventDto)
        try {
            NotificationEventDto notificationEvent = NotificationEventDto.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("VOTE_COMPLETE")
                    .occurredAt(LocalDateTime.now().toString())
                    .producer("vote-service")
                    .data(NotificationEventDto.NotificationData.builder()
                            .receiverId(userId)
                            .category("VOTE")
                            .title("투표 완료")
                            .body(candidate.getName() + "님께 투표하셨습니다.")
                            .deeplink("/votes/" + voteId)
                            .refType("VOTE")
                            .refId(voteId)
                            .attributes(Map.of(
                                    "candidateName", candidate.getName(),
                                    "voteCount", candidate.getVoteCount()
                            ))
                            .build())
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(notificationEvent);
            kafkaTemplate.send("notification-topic", jsonMessage);
            
            log.info("알림 이벤트 발행 성공: userId={}", userId);

        } catch (Exception e) {
            log.error("알림 이벤트 발행 실패", e);
        }
    }
}
