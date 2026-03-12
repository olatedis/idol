package com.bit.idol.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "idols")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Idol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne(fetch = FetchType.LAZY) // 지연 로딩 적용
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩 적용
    @JoinColumn(name = "agency_id")
    private Agency agency;

    @ManyToOne(fetch = FetchType.LAZY) // 그룹 연관관계 추가
    @JoinColumn(name = "group_id")
    @Setter
    private Group group;

    @Setter
    private String stageName;

    @Setter
    @Enumerated(EnumType.STRING)
    private IdolStatus status;
}
