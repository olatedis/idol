package com.bit.idol.userservice.repository;

import com.bit.idol.userservice.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Integer> {

    boolean existsByGroup_IdAndIdol_Id(int groupId, int idolId);
    
    // 그룹 ID로 멤버 조회 (추가됨)
    List<GroupMember> findByGroupId(int groupId);

    void deleteByGroup_IdAndIdol_Id(int groupId, int idolId);
}
