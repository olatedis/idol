package com.bit.docker.boardservice.repository;

import com.bit.docker.boardservice.entity.BoardType;
import com.bit.docker.boardservice.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByBoardTypeOrderByCreatedAtDesc(BoardType boardType, Pageable pageable);

    Page<Post> findByBoardTypeAndIdolIdOrderByCreatedAtDesc(BoardType boardType, Long idolId, Pageable pageable);

    Page<Post> findByBoardTypeAndGroupIdOrderByCreatedAtDesc(BoardType boardType, Long groupId, Pageable pageable);

    // 게시글 조회수 증가
    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.postId = :postId")
    void increaseViewCount(@Param("postId") Long postId);
}
