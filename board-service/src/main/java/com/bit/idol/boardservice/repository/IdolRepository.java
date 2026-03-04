package com.bit.idol.boardservice.repository;

import com.bit.idol.boardservice.entity.Idol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdolRepository extends JpaRepository<Idol, Integer> {
    Optional<Idol> findByUserId(Integer userId);
}