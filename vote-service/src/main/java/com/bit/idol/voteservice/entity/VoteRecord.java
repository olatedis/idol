package com.bit.idol.voteservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "vote_record", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"vote_id", "user_id"}) // DB 수준 중복 방지
})
public class VoteRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "vote_id")
    private int voteId;

    @Column(name = "user_id")
    private int userId;

    @Column(name = "candidate_id")
    private int candidateId;

    private LocalDateTime votedAt = LocalDateTime.now();

    // --- 연관관계 매핑 (QueryDSL 조회용, 읽기 전용) ---
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id", insertable = false, updatable = false)
    private Vote vote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", insertable = false, updatable = false)
    private Candidate candidate;
}
