package com.bit.subscriptionservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 빌링키 엔티티 - 고객의 결제 수단 정보를 안전하게 저장
 * 매월, 매년 정기결제에 사용
 */
@Entity
@Table(name = "billing_keys", indexes = {
        @Index(name = "idx_customer_key", columnList = "customer_key"),
        @Index(name = "idx_user_idol", columnList = "user_id, idol_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String customerKey;  // UUID 형식의 고객 식별자

    @Column(nullable = false)
    private int userId;

    @Column(nullable = false)
    private int idolId;

    @Column(nullable = false)
    private String billingKey;   // Toss에서 발급한 빌링키

    @Column(nullable = false)
    private String cardNumber;   // 마스킹된 카드번호

    @Column(nullable = false)
    private String cardIssuer;   // 카드 발급사

    @Column(nullable = false)
    private String cardType;     // 신용/체크

    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @PrePersist
    public void prePersist() {
        this.issuedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 빌링키를 비활성화 (삭제하지 않고 유지 - 감사 목적)
     */
    public void deactivate() {
        this.active = false;
    }
}
