package com.bit.idol.userservice.repository;

import com.bit.idol.userservice.entity.BanHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BanHistoryRepository extends JpaRepository<BanHistory, Long> {
    List<BanHistory> findByUserIdOrderByCreatedAtDesc(int userId);
}
