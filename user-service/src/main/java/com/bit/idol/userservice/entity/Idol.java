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

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne
    @JoinColumn(name = "agency_id")
    private Agency agency;

    private String stageName;

    @Setter
    @Enumerated(EnumType.STRING)
    private IdolStatus status;
}

