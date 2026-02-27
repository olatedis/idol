package com.bit.idol.boardservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(name = "idx_posts_boardtype", columnList = "board_type"),
                @Index(name = "idx_posts_idol", columnList = "idol_id"),
                @Index(name = "idx_posts_group", columnList = "group_id"),
                @Index(name = "idx_posts_created", columnList = "created_at")
        }
)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", nullable = false, length = 30)
    private BoardType boardType;

    @Column(name = "idol_id")
    private Long idolId; // IDOL_* 게시판에서만 사용

    @Column(name = "group_id")
    private Long groupId; // GROUP_* 게시판에서만 사용

    @Column(name = "author_id", nullable = false)
    private Integer authorId; // 작성자 userId

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "dislike_count", nullable = false)
    private Integer dislikeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;

        if (this.viewCount == null) this.viewCount = 0;
        if (this.likeCount == null) this.likeCount = 0;
        if (this.dislikeCount == null) this.dislikeCount = 0;
        if (this.commentCount == null) this.commentCount = 0;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}