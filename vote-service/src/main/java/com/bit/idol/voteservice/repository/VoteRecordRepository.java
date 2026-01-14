package com.bit.idol.voteservice.repository;

import com.bit.idol.voteservice.entity.VoteRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRecordRepository extends JpaRepository<VoteRecord, Integer>, VoteRecordRepositoryCustom {
    boolean existsByVoteIdAndUserId(Integer voteId, Integer userId);
    Optional<VoteRecord> findByVoteIdAndUserId(Integer voteId, Integer userId);
}
