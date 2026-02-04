package com.bit.concertservice.infra;


import com.bit.concertservice.domain.entity.Concert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConcertRepository extends JpaRepository<Concert, Integer> {
	List<Concert> findByAgencyId(int agencyId);
	List<Concert> findByGroupIdAndActiveTrue(int groupId);
}
