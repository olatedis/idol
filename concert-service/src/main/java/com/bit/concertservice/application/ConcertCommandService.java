package com.bit.concertservice.application;

import com.bit.concertservice.domain.dto.ConcertCreateRequest;
import com.bit.concertservice.domain.dto.ConcertUpdateRequest;
import com.bit.concertservice.domain.dto.NotificationEventDto;
import com.bit.concertservice.domain.entity.Concert;
import com.bit.concertservice.infra.ConcertRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class ConcertCommandService {

    private static final String NOTIFY_TOPIC = "notify-request-topic";

    private final ConcertRepository concertRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public int createConcert(ConcertCreateRequest request) {
        // 입력 검증
        if (request.getAgencyId() <= 0) {
            throw new IllegalArgumentException("유효하지 않은 소속사 ID");
        }
        if (request.getGroupId() <= 0) {
            throw new IllegalArgumentException("그룹은 필수입니다.");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("콘서트 이름은 필수입니다.");
        }
        if (request.getVenue() == null || request.getVenue().isBlank()) {
            throw new IllegalArgumentException("장소는 필수입니다.");
        }
        if (request.getConcertDate() == null) {
            throw new IllegalArgumentException("시작일은 필수입니다.");
        }
        if (request.getTicketSaleDate() == null) {
            throw new IllegalArgumentException("티켓 판매일은 필수입니다.");
        }
        if (request.getTicketSaleDate().isAfter(request.getConcertDate())) {
            throw new IllegalArgumentException("티켓 판매일은 콘서트 시작일보다 먼저여야 합니다.");
        }

        Concert concert = Concert.create(
                request.getAgencyId(),
                request.getGroupId(),
                request.getTitle(),
                request.getDescription(),
                request.getVenue(),
                request.getConcertDate(),
                request.getStartTime(),
                request.getTicketSaleDate()
        );

        Concert saved = concertRepository.save(concert);
        log.info("콘서트 등록 완료: concertId={}, title={}, agencyId={}, groupId={}",
                saved.getId(), saved.getTitle(), saved.getAgencyId(), saved.getGroupId());

        /*String uuid = UUID.randomUUID().toString();
        Map<String, String> map = new HashMap<>();
        map.put("concertName", concert.getTitle());
        map.put("groupId", String.valueOf(concert.getGroupId()));
        map.put("concertId", String.valueOf(concert.getId()));
        map.put("openAt", concert.getConcertDate().toString());
        NotificationEventDto notify =  new NotificationEventDto();
        notify.setEventId(uuid);
        notify.setType("CONCERT_OPENED");
        notify.setTargetType(NotificationEventDto.TargetType.GROUP_SUB);
        notify.setArgs(map);
        notify.setOccurredAt(LocalDateTime.now());
        kafkaTemplate.send("notify-request-topic", notify.toString());*/

        // 알림 발행
        publishConcertNotify("CONCERT_OPENED", saved);

        return saved.getId();
    }

    @Transactional
    public void updateConcert(int concertId, ConcertUpdateRequest request) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("콘서트가 존재하지 않습니다."));

        concert.update(
                request.getTitle(),
                request.getDescription(),
                request.getVenue(),
                request.getConcertDate(),
                request.getStartTime(),
                request.getTicketSaleDate()
        );

        concertRepository.save(concert);

        /*String uuid = UUID.randomUUID().toString();
        Map<String, String> map = new HashMap<>();
        map.put("concertName", concert.getTitle());
        map.put("concertId", String.valueOf(concert.getId()));
        map.put("openAt", concert.getConcertDate().toString());
        NotificationEventDto notify =  new NotificationEventDto();
        notify.setEventId(uuid);
        notify.setType("CONCERT_UPDATED");
        notify.setTargetType(NotificationEventDto.TargetType.GROUP_SUB);
        notify.setArgs(map);
        notify.setOccurredAt(LocalDateTime.now());
        kafkaTemplate.send("notify-request-topic", notify.toString());*/

        // 알림 발행
        publishConcertNotify("CONCERT_UPDATED", concert);

        log.info("콘서트 수정 완료: concertId={}", concertId);
    }

    /**
     * 콘서트 알림 payload 구성
     * - targetType: GROUP_SUB
     * - targetId: groupId(String)
     * - args: 노션 스펙과 동일 키 사용 (concertId, concertName, openAt)
     */
    private void publishConcertNotify(String type, Concert concert) {
        try {
            String eventId = UUID.randomUUID().toString();

            Map<String, String> args = new HashMap<>();
            args.put("concertId", String.valueOf(concert.getId()));
            args.put("concertName", concert.getTitle());
            args.put("openAt", concert.getConcertDate().toString());

            NotificationEventDto payload = NotificationEventDto.builder()
                    .eventId(eventId)
                    .type(type)
                    .targetType(NotificationEventDto.TargetType.GROUP_SUB)
                    .targetId(String.valueOf(concert.getGroupId()))
                    .args(args)
                    .redirectUrl("/concert/" + concert.getId())
                    .occurredAt(LocalDateTime.now())
                    .build();

            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(NOTIFY_TOPIC, json);

            log.info("콘서트 알림 발행 성공: type={}, groupId={}, concertId={}",
                    type, concert.getGroupId(), concert.getId());
        } catch (Exception e) {
            log.error("콘서트 알림 발행 실패: type={}, concertId={}, err={}", type, concert.getId(), e.getMessage());
        }
    }

    // TODO: 여기 아래 삭제랑 비활성화 로직 다시 한번 확인하기.
    @Transactional
    public void deleteConcert(int concertId) {
        if (!concertRepository.existsById(concertId)) {
            throw new IllegalArgumentException("콘서트가 존재하지 않습니다.");
        }
        concertRepository.deleteById(concertId);
        log.info("콘서트 삭제: concertId={}", concertId);
    }

    @Transactional
    public void deactivateConcert(int concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("콘서트가 존재하지 않습니다."));
        concert.deactivate();
        concertRepository.save(concert);
        log.info("콘서트 비활성화: concertId={}", concertId);
    }
}
