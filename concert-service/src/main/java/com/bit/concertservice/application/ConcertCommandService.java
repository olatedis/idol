package com.bit.concertservice.application;

import com.bit.concertservice.domain.dto.ConcertCreateRequest;
import com.bit.concertservice.domain.dto.ConcertUpdateRequest;
import com.bit.concertservice.domain.dto.SeatCreateRequest;
import com.bit.concertservice.domain.entity.Concert;
import com.bit.concertservice.domain.entity.Seat;
import com.bit.concertservice.domain.enumtype.SeatGrade;
import com.bit.concertservice.domain.event.ConcertEvent;
import com.bit.concertservice.infra.ConcertRepository;
import com.bit.concertservice.infra.SeatRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@AllArgsConstructor
@Slf4j
public class ConcertCommandService {

    private final ConcertRepository concertRepository;
    private final SeatRepository seatRepository;
    private final ApplicationEventPublisher eventPublisher; // 변경됨

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
        LocalDateTime ticketSaleDate = request.getTicketSaleDate();
        if (ticketSaleDate == null) {
            ticketSaleDate = request.getConcertDate().minusWeeks(1);
        }
        if (ticketSaleDate.isAfter(request.getConcertDate())) {
            throw new IllegalArgumentException("티켓 판매일은 콘서트 시작일보다 먼저여야 합니다.");
        }

        Concert concert = Concert.create(
                request.getAgencyId(),
                request.getGroupId(),
                request.getTitle(),
                request.getDescription(),
                request.getVenue(),
                request.getImg(),
                request.getConcertDate(),
                request.getStartTime(),
                ticketSaleDate
        );

        Concert saved = concertRepository.save(concert);

        // 좌석 생성
        if (request.getSeats() != null && !request.getSeats().isEmpty()) {
            List<Seat> seatsToSave = new ArrayList<>();
            for (SeatCreateRequest seatReq : request.getSeats()) {
                SeatGrade grade = SeatGrade.valueOf(seatReq.getGrade().toUpperCase());
                for (int i = 1; i <= seatReq.getCount(); i++) {
                    String seatNumber = grade.name() + "-" + String.format("%03d", i);
                    Seat seat = new Seat(seatNumber, grade, seatReq.getPrice(), saved);
                    seatsToSave.add(seat);
                }
            }
            seatRepository.saveAll(seatsToSave);
            log.info("좌석 생성 완료: concertId={}, totalSeats={}", saved.getId(), seatsToSave.size());
        }
        log.info("콘서트 등록 완료: concertId={}, title={}, agencyId={}, groupId={}",
                saved.getId(), saved.getTitle(), saved.getAgencyId(), saved.getGroupId());

        // 알림 발행 (커밋 후 실행됨)
        eventPublisher.publishEvent(new ConcertEvent("CONCERT_OPENED", saved));

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
                request.getImg(),
                request.getConcertDate(),
                request.getStartTime(),
                request.getTicketSaleDate()
        );

        concertRepository.save(concert);

        // 알림 발행 (커밋 후 실행됨)
        eventPublisher.publishEvent(new ConcertEvent("CONCERT_UPDATED", concert));

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
