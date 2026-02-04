package com.bit.idol.boardservice.repository;

import com.bit.idol.boardservice.entity.PostReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {

    Optional<PostReaction> findByPost_PostIdAndUserId(Long postId, Integer userId);

    void deleteByPost_PostId(Long postId);
}
