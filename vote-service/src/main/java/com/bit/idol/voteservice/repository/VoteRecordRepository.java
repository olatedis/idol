package com.bit.idol.voteservice.repository;

import com.bit.idol.voteservice.entity.VoteRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoteRecordRepository extends JpaRepository<VoteRecord, Integer>, VoteRecordRepositoryCustom {
    boolean existsByVoteIdAndUserId(Integer voteId, Integer userId);
    Optional<VoteRecord> findByVoteIdAndUserId(Integer voteId, Integer userId);
    
    // 내 투표 기록 조회 (추가됨)
    List<VoteRecord> findByUserId(Integer userId);
}
