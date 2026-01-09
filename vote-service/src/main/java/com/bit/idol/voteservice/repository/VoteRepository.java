package com.bit.idol.voteservice.repository;

import com.bit.idol.voteservice.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Integer> {
}
