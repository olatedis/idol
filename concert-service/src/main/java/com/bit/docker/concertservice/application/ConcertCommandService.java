package com.bit.docker.concertservice.application;

import com.bit.docker.concertservice.domain.dto.ConcertCreateRequest;
import com.bit.docker.concertservice.domain.entity.Concert;
import com.bit.docker.concertservice.infra.ConcertRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class ConcertCommandService {

    private final ConcertRepository concertRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public int createConcert(ConcertCreateRequest request) {
        // 입력 검증
        if (request.getAgencyId() <= 0) {
            throw new IllegalArgumentException("유효하지 않은 소속사 ID");
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
                request.getTitle(),
                request.getDescription(),
                request.getVenue(),
                request.getConcertDate(),
                request.getStartTime(),
                request.getTicketSaleDate()
        );

        Concert saved = concertRepository.save(concert);
        log.info("콘서트 등록 완료: concertId={}, title={}, agencyId={}", saved.getId(), saved.getTitle(), saved.getAgencyId());

        String uuid = UUID.randomUUID().toString();
        String message = uuid+":"+concert.getId()+":"+request.getTitle()+":"+request.getStartTime();
        kafkaTemplate.send("CONCERT_OPENED", message);

        return saved.getId();
    }

    @Transactional
    public void updateConcert(int concertId, com.bit.docker.concertservice.domain.dto.ConcertUpdateRequest request) {
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

        String uuid = UUID.randomUUID().toString();
        String message = uuid+":"+concert.getId()+":"+request.getTitle()+":"+request.getStartTime();
        kafkaTemplate.send("CONCERT_UPDATED", message);

        log.info("콘서트 수정 완료: concertId={}", concertId);
    }

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
