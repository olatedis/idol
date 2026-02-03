package com.bit.idol.userservice.service;

import com.bit.idol.userservice.dto.idol.IdolDto;
import com.bit.idol.userservice.entity.GroupMember;
import com.bit.idol.userservice.repository.GroupMemberRepository;
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

    public List<IdolDto> getIdolsByGroup(int groupId) {
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        
        return members.stream()
                .map(member -> IdolDto.fromEntity(member.getIdol()))
                .collect(Collectors.toList());
    }
}
