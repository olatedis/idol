package com.bit.idol.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agency_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_agency_accounts_user", columnNames = {"user_id"})
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // 소속사 운영 계정(User.role=AGENCY)
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 어떤 소속사(Agency)에 속하는지
    @ManyToOne
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;
}
