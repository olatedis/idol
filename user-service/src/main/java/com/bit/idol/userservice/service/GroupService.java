package com.bit.idol.userservice.service;

import com.bit.idol.userservice.dto.group.GroupDto;
import com.bit.idol.userservice.dto.idol.IdolDto;
import com.bit.idol.userservice.entity.AgencyAccount;
import com.bit.idol.userservice.entity.Group;
import com.bit.idol.userservice.entity.GroupMember;
import com.bit.idol.userservice.repository.AgencyAccountRepository;
import com.bit.idol.userservice.repository.GroupMemberRepository;
import com.bit.idol.userservice.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository; // 추가됨
    private final com.bit.idol.userservice.repository.IdolRepository idolRepository;
    private final com.bit.idol.userservice.repository.AgencyAccountRepository agencyAccountRepository;
    private final S3Service s3Service;


    // 그룹 소속 아이돌 목록 조회 (기존 메서드 유지)
    public List<IdolDto> getIdolsByGroup(int groupId) {
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        
        return members.stream()
                .map(member -> IdolDto.fromEntity(member.getIdol()))
                .collect(Collectors.toList());
    }

    // 에이전시 관리 그룹 목록 조회
    public List<GroupDto> getManagedGroups(int userId) {
        return agencyAccountRepository.findByUser_Id(userId)
                .map(account -> groupRepository.findByAgency_Id(account.getAgency().getId()))
                .orElse(List.of())
                .stream()
                .map(group -> GroupDto.fromEntity(group, null))
                .collect(Collectors.toList());
    }

    // 그룹 상세 조회 (추가됨)
    public GroupDto getGroup(int groupId) {
        Group group = groupRepository.findByIdWithAgency(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // 멤버 목록도 같이 조회
        List<IdolDto> members = getIdolsByGroup(groupId);

        return GroupDto.fromEntity(group, members);
    }

    // 전체 그룹 목록 조회 (추가됨)
    public List<GroupDto> getAllGroups() {
        return groupRepository.findAllWithAgency().stream()
                .map(group -> GroupDto.fromEntity(group, null))
                .collect(Collectors.toList());
    }

    // 벌크 조회 (N+1 방지용)
    public List<GroupDto> getGroupsByIds(List<Integer> ids) {
        return groupRepository.findAllById(ids).stream()
                .map(group -> GroupDto.fromEntity(group, null))
                .collect(Collectors.toList());
    }

    // 그룹에 추가 가능한 아이돌 목록 (해당 소속사 소속 + 미소속 아이돌)
    public List<IdolDto> getAvailableIdolsForGroup(int groupId) {
        Group group = groupRepository.findByIdWithAgency(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return idolRepository.findAvailableIdolsByAgencyId(group.getAgency().getId())
                .stream()
                .map(idol -> IdolDto.fromEntity(idol))
                .collect(Collectors.toList());
    }

    @Transactional
    public void addMemberToGroup(int groupId, int idolId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        com.bit.idol.userservice.entity.Idol idol = idolRepository.findById(idolId)
                .orElseThrow(() -> new RuntimeException("Idol not found"));

        if (groupMemberRepository.existsByGroup_IdAndIdol_Id(groupId, idolId)) {
            throw new RuntimeException("Already a member");
        }

        groupMemberRepository.save(GroupMember.builder()
                .group(group)
                .idol(idol)
                .build());

        idol.setGroup(group);
    }

    @Transactional
    public void removeMemberFromGroup(int groupId, int idolId) {
        groupMemberRepository.deleteByGroup_IdAndIdol_Id(groupId, idolId);

        com.bit.idol.userservice.entity.Idol idol = idolRepository.findById(idolId)
                .orElseThrow(() -> new RuntimeException("Idol not found"));
        idol.setGroup(null);
    }

    @Transactional
    public String updateGroupImage(int groupId, org.springframework.web.multipart.MultipartFile file) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        try {
            String fileUrl = s3Service.uploadFile(file);
            group.setGroupImage(fileUrl);
            groupRepository.saveAndFlush(group);
            return fileUrl;
        } catch (Exception e) {
            throw new RuntimeException("이미지 업로드 실패", e);
        }
    }
}
