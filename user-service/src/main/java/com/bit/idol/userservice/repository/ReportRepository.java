package com.bit.idol.userservice.repository;

import com.bit.idol.userservice.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByTargetUserId(int targetUserId);
}
