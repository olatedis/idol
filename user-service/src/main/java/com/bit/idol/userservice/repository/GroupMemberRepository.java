package com.bit.idol.userservice.repository;

import com.bit.idol.userservice.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Integer> {

    boolean existsByGroup_IdAndIdol_Id(int groupId, int idolId);
}
