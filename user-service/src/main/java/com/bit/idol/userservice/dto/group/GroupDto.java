package com.bit.idol.userservice.dto.group;

import com.bit.idol.userservice.dto.idol.IdolDto;
import com.bit.idol.userservice.entity.Group;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDto {

    private int groupId;
    private String name;
    private String groupImage; // 그룹 이미지 필드 추가
    private int agencyId;
    private String agencyName;
    private List<IdolDto> members; // 그룹 멤버 목록

    public static GroupDto fromEntity(Group group, List<IdolDto> members) {
        return GroupDto.builder()
                .groupId(group.getId())
                .name(group.getName())
                .groupImage(group.getGroupImage())
                .agencyId(group.getAgency().getId())
                .agencyName(group.getAgency().getName())
                .members(members)
                .build();
    }
}
