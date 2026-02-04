package com.bit.idol.boardservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "post_reactions",
        uniqueConstraints = {
                // 유저당 게시글 반응 1개 강제
                @UniqueConstraint(name = "uk_post_reactions_post_user", columnNames = {"post_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_post_reactions_post", columnList = "post_id"),
                @Index(name = "idx_post_reactions_user", columnList = "user_id")
        }
)
public class PostReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reactionId;

    // 어떤 게시글에 대한 반응인지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 반응한 유저
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    // LIKE / DISLIKE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReactionType reactionType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
