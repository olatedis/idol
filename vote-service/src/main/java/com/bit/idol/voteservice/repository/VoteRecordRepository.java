package com.bit.idol.voteservice.repository;

import com.bit.idol.voteservice.entity.VoteRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRecordRepository extends JpaRepository<VoteRecord,Integer> {
    boolean existsByVoteIdAndUserId(Integer voteId, Integer userId);
}
