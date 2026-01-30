package com.bit.docker.boardservice.repository;

import com.bit.docker.boardservice.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 최신 댓글이 위로 오도록 DESC 정렬
    List<Comment> findByPost_PostIdOrderByCreatedAtDesc(Long postId);

    // 게시글 하드삭제 시, 댓글도 하드삭제
    void deleteByPost_PostId(Long postId);
}
