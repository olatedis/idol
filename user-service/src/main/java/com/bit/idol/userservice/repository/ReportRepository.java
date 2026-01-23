package com.bit.idol.userservice.repository;

import com.bit.idol.userservice.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByTargetUserId(int targetUserId);
    
    // 중복 신고 체크 (이미 신고했는지 확인)
    boolean existsByReporterIdAndTargetUserId(int reporterId, int targetUserId);
}
