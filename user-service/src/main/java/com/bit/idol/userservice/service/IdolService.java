package com.bit.idol.userservice.service;

import com.bit.idol.userservice.dto.idol.IdolDto;
import com.bit.idol.userservice.dto.idol.IdolRegisterRequest;
import com.bit.idol.userservice.entity.*;
import com.bit.idol.userservice.repository.AgencyRepository;
import com.bit.idol.userservice.repository.IdolRepository;
import com.bit.idol.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        if (request.getAgencyId() != null) {
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

        return IdolDto.fromEntity(idol);
    }

    public IdolDto getIdol(Long idolId) {
        Idol idol = idolRepository.findById(idolId)
                .orElseThrow(() -> new RuntimeException("Idol not found"));
        return IdolDto.fromEntity(idol);
    }

    public List<IdolDto> getAllIdols() {
        return idolRepository.findAll().stream()
                .map(IdolDto::fromEntity)
                .toList();
    }

    @Transactional
    public void changeIdolStatus(Long idolId, IdolStatus status) {
        Idol idol = idolRepository.findById(idolId)
                .orElseThrow(() -> new RuntimeException("Idol not found"));
        idol.setStatus(status);
        log.info("아이돌 상태 변경: idolId={}, status={}", idolId, status);
    }
}

