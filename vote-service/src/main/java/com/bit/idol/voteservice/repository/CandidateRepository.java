package com.bit.idol.voteservice.repository;

import com.bit.idol.voteservice.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, Integer> {
    List<Candidate> findByVoteId(int voteId);
    Optional<Candidate> findByCandidateNumber(int candidateNumber);
    Optional<Candidate> findByVoteIdAndCandidateNumber(int voteId, int candidateNumber);
}
