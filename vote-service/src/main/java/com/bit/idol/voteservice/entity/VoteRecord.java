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

    private int candidateId;
    private LocalDateTime votedAt = LocalDateTime.now();
}