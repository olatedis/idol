package com.bit.idol.userservice.service;

import com.bit.idol.userservice.entity.AgencyAccount;
import com.bit.idol.userservice.entity.Group;
import com.bit.idol.userservice.entity.Idol;
import com.bit.idol.userservice.repository.AgencyAccountRepository;
import com.bit.idol.userservice.repository.GroupRepository;
import com.bit.idol.userservice.repository.GroupMemberRepository;
import com.bit.idol.userservice.repository.IdolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalValidationService {

    private final IdolRepository idolRepository;
    private final AgencyAccountRepository agencyAccountRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;


    public boolean isIdolOwner(int idolId, int userId) {
        return idolRepository.findById(idolId)
                .map(idol -> idol.getUser().getId() == userId)
                .orElse(false);
    }

    public boolean canAgencyManageIdol(int agencyUserId, int idolId) {
        AgencyAccount account = agencyAccountRepository.findByUser_Id(agencyUserId).orElse(null);
        if (account == null) return false;

        Idol idol = idolRepository.findById(idolId).orElse(null);
        if (idol == null) return false;
        if (idol.getAgency() == null) return false;

        return idol.getAgency().getId() == account.getAgency().getId();
    }

    // userId(IDOL) -> Idol.id로 변환 후 group_member 존재 확인
    public boolean isGroupMember(int groupId, int userId) {
        Idol idol = idolRepository.findByUser_Id(userId).orElse(null);
        if (idol == null) return false;

        return groupMemberRepository.existsByGroup_IdAndIdol_Id(groupId, idol.getId());
    }

    // 그룹은 단일 소속사에 소속되며, 소속사 계정은 "그룹의 agency"와 같으면 관리 가능
    public boolean canAgencyManageGroup(int agencyUserId, int groupId) {
        AgencyAccount account = agencyAccountRepository.findByUser_Id(agencyUserId).orElse(null);
        if (account == null) return false;

        Group group = groupRepository.findById(groupId).orElse(null);
        if (group == null) return false;

        return group.getAgency().getId() == account.getAgency().getId();
    }
}
