package com.bit.docker.concertservice.infra;


import com.bit.docker.concertservice.domain.entity.Concert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertRepository extends JpaRepository<Concert, Integer> {
}
