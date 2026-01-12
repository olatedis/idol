package com.bit.idol.voteservice.repository;

import com.bit.idol.voteservice.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, Integer> {
    List<Candidate> findByVoteId(int voteId);
    Optional<Candidate> findByCandidateNumber(int candidateNumber);
    Optional<Candidate> findByVoteIdAndCandidateNumber(int voteId, int candidateNumber);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Candidate c SET c.voteCount = c.voteCount + 1 WHERE c.id = :id")
    void incrementVoteCount(@Param("id") int id);
}
