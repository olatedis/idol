package com.bit.docker.boardservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_boardtype", columnList = "boardType"),
        @Index(name = "idx_posts_idol", columnList = "idolId"),
        @Index(name = "idx_posts_group", columnList = "groupId"),
        @Index(name = "idx_posts_created", columnList = "createdAt")
})
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BoardType boardType;

    private Long idolId; // IDOL_* 게시판에서만 사용

    private Long groupId; // GROUP_* 게시판에서만 사용

    @Column(nullable = false)
    private Integer authorId; // 작성자 userId

    @Column(nullable = false, length = 200)
    private String title;

   @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer viewCount = 0;

    @Column(nullable = false)
    private Integer likeCount = 0;

    @Column(nullable = false)
    private Integer dislikeCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.viewCount == null) this.viewCount = 0;
        if (this.likeCount == null) this.likeCount = 0;
        if (this.dislikeCount == null) this.dislikeCount = 0;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}