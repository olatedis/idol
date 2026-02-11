package com.bit.idol.boardservice.repository;

import com.bit.idol.boardservice.entity.BoardType;
import com.bit.idol.boardservice.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    // IDOL_*
    Page<Post> findByBoardTypeAndIdolIdOrderByCreatedAtDesc(BoardType boardType, Long idolId, Pageable pageable);
    Page<Post> findByBoardTypeAndIdolIdOrderByLikeCountDesc(BoardType boardType, Long idolId, Pageable pageable);

    // GROUP_*
    Page<Post> findByBoardTypeAndGroupIdOrderByCreatedAtDesc(BoardType boardType, Long groupId, Pageable pageable);
    Page<Post> findByBoardTypeAndGroupIdOrderByLikeCountDesc(BoardType boardType, Long groupId, Pageable pageable);

    // ADMIN_NOTICE 등
    Page<Post> findByBoardTypeOrderByCreatedAtDesc(BoardType boardType, Pageable pageable);
    Page<Post> findByBoardTypeOrderByLikeCountDesc(BoardType boardType, Pageable pageable);

    // 조회수 증가 (동시성 고려 X -> 단순 증가)
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.postId = :postId")
    void increaseViewCount(@Param("postId") Long postId);

    // 좋아요 증가
    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.postId = :postId")
    void incrementLikeCount(@Param("postId") Long postId);

    // 좋아요 감소
    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.postId = :postId")
    void decrementLikeCount(@Param("postId") Long postId);

    // 싫어요 증가
    @Modifying
    @Query("UPDATE Post p SET p.dislikeCount = p.dislikeCount + 1 WHERE p.postId = :postId")
    void incrementDislikeCount(@Param("postId") Long postId);

    // 싫어요 감소
    @Modifying
    @Query("UPDATE Post p SET p.dislikeCount = p.dislikeCount - 1 WHERE p.postId = :postId")
    void decrementDislikeCount(@Param("postId") Long postId);
}
