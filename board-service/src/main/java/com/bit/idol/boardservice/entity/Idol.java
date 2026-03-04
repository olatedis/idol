package com.bit.idol.boardservice.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "idols")
@Data
public class Idol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 기존 테이블 컬럼명: stage_name
    @Column(name = "stage_name")
    private String stageName;

    // 기존 테이블 enum: ACTIVE/INACTIVE/PENDING
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private IdolStatus status;

    // FK는 관계로 안 걸고(지금 목적상 불필요), 컬럼만 들고 가도 안정적입니다.
    @Column(name = "agency_id")
    private Integer agencyId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Integer userId;

    @Column(name = "group_id")
    private Integer groupId;

    public enum IdolStatus {
        ACTIVE, INACTIVE, PENDING
    }
}