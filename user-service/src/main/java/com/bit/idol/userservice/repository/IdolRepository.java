package com.bit.idol.userservice.repository;

import com.bit.idol.userservice.entity.Idol;
import com.bit.idol.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IdolRepository extends JpaRepository<Idol,Integer> {
    boolean existsByUser(User user);

    Optional<Idol> findByUser_Id(int userId);

    // N+1 문제 해결을 위한 Fetch Join
    @Query("SELECT i FROM Idol i LEFT JOIN FETCH i.group LEFT JOIN FETCH i.agency LEFT JOIN FETCH i.user")
    List<Idol> findAllWithDetails();
}
