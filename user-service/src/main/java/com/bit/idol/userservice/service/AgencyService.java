package com.bit.idol.userservice.service;

import com.bit.idol.userservice.dto.agency.AgencyCreateRequest;
import com.bit.idol.userservice.dto.agency.AgencyDto;
import com.bit.idol.userservice.entity.Agency;
import com.bit.idol.userservice.repository.AgencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AgencyService {

    private final AgencyRepository agencyRepository;

    @Transactional
    public AgencyDto createAgency(AgencyCreateRequest request) {
        Agency agency = Agency.builder()
                .name(request.getName())
                .build();

        agencyRepository.save(agency);
        log.info("소속사 생성 완료: agencyId={}", agency.getId());

        return AgencyDto.fromEntity(agency);
    }

    public List<AgencyDto> getAllAgencies() {
        return agencyRepository.findAll().stream()
                .map(AgencyDto::fromEntity)
                .toList();
    }
}
