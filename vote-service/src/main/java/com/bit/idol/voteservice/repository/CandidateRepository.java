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
    
    // 워밍업용
    List<Candidate> findAllByVoteId(int voteId);

    Optional<Candidate> findByNumber(int number); // candidateNumber -> number
    Optional<Candidate> findByVoteIdAndNumber(int voteId, int number); // candidateNumber -> number

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Candidate c SET c.voteCount = c.voteCount + 1 WHERE c.id = :id")
    void incrementVoteCount(@Param("id") int id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Candidate c SET c.voteCount = c.voteCount - 1 WHERE c.id = :id AND c.voteCount > 0")
    void decrementVoteCount(@Param("id") int id);
}
