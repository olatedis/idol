package com.bit.idol.userservice.service;

import com.bit.idol.userservice.dto.event.IdolEvent;
import com.bit.idol.userservice.dto.idol.IdolDto;
import com.bit.idol.userservice.dto.idol.IdolRegisterRequest;
import com.bit.idol.userservice.entity.*;
import com.bit.idol.userservice.repository.AgencyRepository;
import com.bit.idol.userservice.repository.IdolRepository;
import com.bit.idol.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class IdolService {

    private final IdolRepository idolRepository;
    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public IdolDto registerIdol(IdolRegisterRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.IDOL) {
            throw new RuntimeException("IDOL 권한을 가진 유저만 등록할 수 있습니다.");
        }

        if (idolRepository.existsByUser(user)) {
            throw new RuntimeException("이미 아이돌로 등록된 유저입니다.");
        }

        Agency agency = null;
        if (request.getAgencyId() != 0) {
            agency = agencyRepository.findById(request.getAgencyId())
                    .orElseThrow(() -> new RuntimeException("Agency not found"));
        }

        Idol idol = Idol.builder()
                .user(user)
                .agency(agency)
                .stageName(request.getStageName())
                .status(IdolStatus.ACTIVE)
                .build();

        idolRepository.save(idol);
        log.info("아이돌 등록 완료: idolId={}, userId={}", idol.getId(), user.getId());

        // Kafka 이벤트 발행
        sendIdolEvent("CREATE", idol);

        return IdolDto.fromEntity(idol);
    }

    public IdolDto getIdol(int idolId) {
        Idol idol = idolRepository.findById(idolId)
                .orElseThrow(() -> new RuntimeException("Idol not found"));
        return IdolDto.fromEntity(idol);
    }

    public IdolDto getIdolByUserId(int userId) {
        Idol idol = idolRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Idol not found for user: " + userId));
        return IdolDto.fromEntity(idol);
    }

    public List<IdolDto> getAllIdols() {
        // N+1 문제 해결을 위해 Fetch Join 사용
        return idolRepository.findAllWithDetails().stream()
                .map(IdolDto::fromEntity)
                .toList();
    }

    @Transactional
    public void changeIdolStatus(int idolId, IdolStatus status) {
        Idol idol = idolRepository.findById(idolId)
                .orElseThrow(() -> new RuntimeException("Idol not found"));
        idol.setStatus(status);
        log.info("아이돌 상태 변경: idolId={}, status={}", idolId, status);

        // Kafka 이벤트 발행
        sendIdolEvent("UPDATE", idol);
    }

    private void sendIdolEvent(String type, Idol idol) {
        try {
            IdolEvent event = IdolEvent.builder()
                    .type(type)
                    .idolId(idol.getId())
                    .stageName(idol.getStageName())
                    .profileImage(idol.getUser().getImgUrl())
                    .status(idol.getStatus().name())
                    .build();

            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("idol-events", message);
        } catch (Exception e) {
            log.error("아이돌 이벤트 발행 실패: {}", e.getMessage());
        }
    }
}
