package com.bit.idol.voteservice.repository;

import com.bit.idol.voteservice.entity.Vote;
import com.bit.idol.voteservice.entity.VoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Integer> {
    List<Vote> findAllByEndDateBeforeAndStatus(LocalDateTime now, VoteStatus status);
    Page<Vote> findByTitleContaining(String keyword, Pageable pageable);
    
    // 상태별 투표 목록 조회 (워밍업용)
    List<Vote> findAllByStatus(VoteStatus status);
}
