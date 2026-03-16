package com.bit.idol.userservice.service;

import com.bit.idol.userservice.dto.agency.AgencyCreateRequest;
import com.bit.idol.userservice.dto.agency.AgencyDto;
import com.bit.idol.userservice.dto.agency.AgencyUpdateRequest;
import com.bit.idol.userservice.entity.Agency;
import com.bit.idol.userservice.entity.AgencyAccount;
import com.bit.idol.userservice.repository.AgencyAccountRepository;
import com.bit.idol.userservice.repository.AgencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AgencyService {

    private final AgencyRepository agencyRepository;
    private final AgencyAccountRepository agencyAccountRepository;

    public int getAgencyId(int userId){
        Optional<AgencyAccount> agencyAccount= agencyAccountRepository.findByUser_Id(userId);
       if(agencyAccount.isEmpty()){
           return -1;
       }
       AgencyAccount agencyId = agencyAccount.get();
       return agencyId.getAgency().getId();
    }

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

    @Transactional
    public AgencyDto updateAgency(int agencyId, AgencyUpdateRequest request) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("소속사를 찾을 수 없습니다."));

        agency.setName(request.getName());
        return AgencyDto.fromEntity(agency);
    }

    @Transactional
    public void deleteAgency(int agencyId) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("소속사를 찾을 수 없습니다."));

        // 연쇄 삭제: 해당 소속사의 소속사 계정(AgencyAccount)들 모두 삭제
        List<AgencyAccount> accounts = agencyAccountRepository.findByAgency_Id(agencyId);
        if (!accounts.isEmpty()) {
            agencyAccountRepository.deleteAll(accounts);
            log.info("소속사 연쇄 삭제: agencyId={}의 계정 {}개 삭제됨", agencyId, accounts.size());
        }

        agencyRepository.delete(agency);
        log.info("소속사 삭제 완료: agencyId={}", agencyId);
    }
}
