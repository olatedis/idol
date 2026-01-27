package com.bit.docker.boardservice.repository;

import com.bit.docker.boardservice.entity.BoardType;
import com.bit.docker.boardservice.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 이건 거의 안쓸거같긴함
    Page<Post> findByBoardTypeOrderByCreatedAtDesc(BoardType boardType, Pageable pageable);

    Page<Post> findByBoardTypeAndIdolIdOrderByCreatedAtDesc(BoardType boardType, Long idolId, Pageable pageable);

    Page<Post> findByBoardTypeAndGroupIdOrderByCreatedAtDesc(BoardType boardType, Long groupId, Pageable pageable);
}
