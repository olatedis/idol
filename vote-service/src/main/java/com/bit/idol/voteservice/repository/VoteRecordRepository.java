package com.bit.idol.voteservice.repository;

import com.bit.idol.voteservice.entity.VoteRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoteRecordRepository extends JpaRepository<VoteRecord, Integer>, VoteRecordRepositoryCustom {
    boolean existsByVoteIdAndUserId(Integer voteId, Integer userId);
    Optional<VoteRecord> findByVoteIdAndUserId(Integer voteId, Integer userId);
    
    // 내 투표 기록 조회 (엔티티 전체 조회 - 마이페이지용)
    List<VoteRecord> findByUserId(Integer userId);

    // 내 투표 ID 목록만 조회 (투표 목록용 - 성능 최적화)
    @Query("SELECT vr.voteId FROM VoteRecord vr WHERE vr.userId = :userId")
    List<Integer> findVoteIdsByUserId(@Param("userId") Integer userId);
}
