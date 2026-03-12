package com.bit.idol.userservice.repository;

import com.bit.idol.userservice.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Integer> {

    // N+1 문제 해결을 위한 Fetch Join (전체 조회)
    @Query("SELECT g FROM Group g JOIN FETCH g.agency")
    List<Group> findAllWithAgency();

    // N+1 문제 해결을 위한 Fetch Join (상세 조회)
    @Query("SELECT g FROM Group g JOIN FETCH g.agency WHERE g.id = :id")
    Optional<Group> findByIdWithAgency(@Param("id") int id);

    List<Group> findByAgency_Id(int agencyId);

    // 소속사 ID로 그룹 목록 조회
    List<Group> findByAgencyId(int agencyId);
}
