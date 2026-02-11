package com.bit.idol.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "idol_groups") // 예약어 회피
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩 적용
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;
}
