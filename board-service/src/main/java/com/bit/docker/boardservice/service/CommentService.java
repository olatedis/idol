package com.bit.docker.boardservice.service;

import com.bit.docker.boardservice.client.SubscriptionInternalClient;
import com.bit.docker.boardservice.client.UserInternalClient;
import com.bit.docker.boardservice.dto.comment.CommentResponse;
import com.bit.docker.boardservice.dto.comment.CommentUpdateRequest;
import com.bit.docker.boardservice.dto.comment.CommentWriteRequest;
import com.bit.docker.boardservice.entity.BoardType;
import com.bit.docker.boardservice.entity.Comment;
import com.bit.docker.boardservice.entity.Post;
import com.bit.docker.boardservice.repository.CommentRepository;
import com.bit.docker.boardservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    private final SubscriptionInternalClient subscriptionInternalClient;
    private final UserInternalClient userInternalClient;

    // 댓글 작성
    @Transactional
    public CommentResponse write(Long postId, CommentWriteRequest req, Integer userId, Role role) {
        Post post = getPost(postId);

        requireReadSubscription(post, userId, role);

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthorId(userId);
        comment.setContent(req.getContent());

        Comment saved = commentRepository.save(comment);
        return toResponse(saved);
    }

    // 댓글 목록 조회
    @Transactional(readOnly = true)
    public List<CommentResponse> showAll(Long postId, Integer userId, Role role) {
        Post post = getPost(postId);

        requireReadSubscription(post, userId, role);

        return commentRepository.findByPost_PostIdOrderByCreatedAtDesc(postId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 댓글 수정
    @Transactional
    public CommentResponse update(Long commentId, CommentUpdateRequest req, Integer userId, Role role) {
        Comment comment = getComment(commentId);

        if (!canModify(comment, userId, role)) {
            throw new RuntimeException("접근 권한이 없습니다.");
        }

        comment.setContent(req.getContent());
        return toResponse(comment);
    }

    // 댓글 삭제
    @Transactional
    public void delete(Long commentId, Integer userId, Role role) {
        Comment comment = getComment(commentId);

        if (!canDelete(comment, userId, role)) {
            throw new RuntimeException("접근 권한이 없습니다.");
        }

        comment.setIsDeleted(true);
        comment.setDeletedAt(LocalDateTime.now());
    }

    // ================== 내부 로직 ==================

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
    }

    private Comment getComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
    }

    // 게시글 상세 조회 권환
    private void requireReadSubscription(Post post, Integer userId, Role role) {
        if (role == Role.ADMIN) return;

        if (post.getBoardType() == BoardType.IDOL_OFFICIAL || post.getBoardType() == BoardType.IDOL_FAN) {
            if (!subscriptionInternalClient.isActiveIdolSubscriber(post.getIdolId(), userId)) {
                throw new RuntimeException("구독이 필요합니다.");
            }
            return;
        }

        if (post.getBoardType() == BoardType.GROUP_OFFICIAL || post.getBoardType() == BoardType.GROUP_FAN) {
            if (!subscriptionInternalClient.isActiveGroupSubscriber(post.getGroupId(), userId)) {
                throw new RuntimeException("구독이 필요합니다.");
            }
        }
    }

    // 수정 권한 판단
    private boolean canModify(Comment comment, Integer userId, Role role) {
        if (role == Role.ADMIN) return true;
        return comment.getAuthorId().equals(userId);
    }

    // 삭제 권한 판단
    private boolean canDelete(Comment comment, Integer userId, Role role) {
        if (role == Role.ADMIN) return true;
        if (comment.getAuthorId().equals(userId)) return true;

        Post post = comment.getPost();

        // OFFICIAL 게시판만 IDOL/AGENCY 추가 권한
        if (post.getBoardType() == BoardType.IDOL_OFFICIAL) {
            if (role == Role.IDOL) {
                return userInternalClient.isIdolOwner(post.getIdolId(), userId);
            }
            if (role == Role.AGENCY) {
                return userInternalClient.canAgencyManageIdol(userId, post.getIdolId());
            }
        }

        if (post.getBoardType() == BoardType.GROUP_OFFICIAL) {
            if (role == Role.IDOL) {
                return userInternalClient.isGroupMember(post.getGroupId(), userId);
            }
            if (role == Role.AGENCY) {
                return userInternalClient.canAgencyManageGroup(userId, post.getGroupId());
            }
        }

        return false;
    }

    // Comment → CommentResponse 변환
    // 소프트 삭제된 댓글은 content를 "삭제된 댓글입니다"로 치환
    private CommentResponse toResponse(Comment c) {
        CommentResponse res = new CommentResponse();
        res.setCommentId(c.getCommentId());
        res.setAuthorId(c.getAuthorId());

        boolean deleted = Boolean.TRUE.equals(c.getIsDeleted());
        res.setIsDeleted(deleted);
        res.setContent(deleted ? "삭제된 댓글입니다" : c.getContent());

        res.setCreatedAt(c.getCreatedAt());
        res.setUpdatedAt(c.getUpdatedAt());
        return res;
    }
}
