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

    // 마감 임박 투표 조회 (추가됨)
    List<Vote> findAllByEndDateBetweenAndStatus(LocalDateTime start, LocalDateTime end, VoteStatus status);

    // 기존: 제목 기반 전체 페이징
    Page<Vote> findByTitleContaining(String keyword, Pageable pageable);

    // 신규: 그룹 필터링 전용 페이징 (keyword 포함/미포함 모두)
    Page<Vote> findByTargetGroupIdAndTitleContaining(Long targetGroupId, String keyword, Pageable pageable);

    Page<Vote> findByTargetGroupId(Long targetGroupId, Pageable pageable);

    // 상태별 투표 목록 조회 (워밍업용)
    List<Vote> findAllByStatus(VoteStatus status);
}
