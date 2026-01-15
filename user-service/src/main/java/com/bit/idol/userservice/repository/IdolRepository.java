package com.bit.idol.userservice.repository;

import com.bit.idol.userservice.entity.Idol;
import com.bit.idol.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdolRepository extends JpaRepository<Idol,Integer> {
    boolean existsByUser(User user);
}
