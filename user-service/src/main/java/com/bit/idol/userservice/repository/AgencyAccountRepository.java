package com.bit.idol.userservice.repository;

import com.bit.idol.userservice.entity.AgencyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgencyAccountRepository extends JpaRepository<AgencyAccount, Integer> {
    Optional<AgencyAccount> findByUser_Id(int userId);

    List<AgencyAccount> findByAgency_Id(int agencyId);
}
