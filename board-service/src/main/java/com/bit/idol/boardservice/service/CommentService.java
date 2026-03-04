package com.bit.idol.boardservice.service;

import com.bit.idol.boardservice.client.SubscriptionInternalClient;
import com.bit.idol.boardservice.client.UserInternalClient;
import com.bit.idol.boardservice.dto.comment.CommentResponse;
import com.bit.idol.boardservice.dto.comment.CommentUpdateRequest;
import com.bit.idol.boardservice.dto.comment.CommentWriteRequest;
import com.bit.idol.boardservice.entity.BoardType;
import com.bit.idol.boardservice.entity.Comment;
import com.bit.idol.boardservice.entity.Idol;
import com.bit.idol.boardservice.entity.Post;
import com.bit.idol.boardservice.repository.CommentRepository;
import com.bit.idol.boardservice.repository.IdolRepository;
import com.bit.idol.boardservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    private final SubscriptionInternalClient subscriptionInternalClient;
    private final UserInternalClient userInternalClient;

    // IDOL 본인 체크용 (board DB idols)
    private final IdolRepository idolRepository;

    // 댓글 작성
    @Transactional
    public CommentResponse write(Long postId, CommentWriteRequest req, Integer userId, Role role) {
        Post post = getPost(postId);

        requireReadSubscription(post, userId, role);

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthorId(userId);
        comment.setAuthorNickname(req.getNickname()); // 닉네임 저장
        comment.setContent(req.getContent());

        commentRepository.save(comment);

        // 댓글 수 증가 (삭제 제외 정책)
        post.setCommentCount(post.getCommentCount() + 1);

        return toResponse(comment);
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

        if (Boolean.TRUE.equals(comment.getIsDeleted())) {
            return;
        }

        comment.setIsDeleted(true);
        comment.setDeletedAt(LocalDateTime.now());

        // 댓글 수 감소
        Post post = comment.getPost();
        post.setCommentCount(post.getCommentCount() - 1);
    }

    // 내부 로직

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
    }

    private Comment getComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
    }

    // 게시글 읽기권한 정책 통일 (IDOL_FAN 제거 + IDOL/AGENCY 예외)
    private void requireReadSubscription(Post post, Integer userId, Role role) {

        // 공지사항은 전체 공개
        if (post.getBoardType() == BoardType.ADMIN_NOTICE) return;

        if (role == Role.ADMIN) return;

        // IDOL_OFFICIAL: IDOL(본인)/AGENCY는 통과, USER는 구독자만
        if (post.getBoardType() == BoardType.IDOL_OFFICIAL) {

            if (role == Role.IDOL) {
                if (isMyIdol(post.getIdolId(), userId)) return;
            }
            if (role == Role.AGENCY) return;

            if (!subscriptionInternalClient.isActiveIdolSubscriber(post.getIdolId(), userId)) {
                throw new RuntimeException("구독이 필요합니다.");
            }
            return;
        }

        // GROUP_OFFICIAL / GROUP_FAN
        if (post.getBoardType() == BoardType.GROUP_OFFICIAL || post.getBoardType() == BoardType.GROUP_FAN) {

            // GROUP_OFFICIAL은 IDOL/AGENCY 통과(정책)
            if (post.getBoardType() == BoardType.GROUP_OFFICIAL) {
                if (role == Role.IDOL || role == Role.AGENCY) return;
            }

            if (!subscriptionInternalClient.isActiveGroupSubscriber(post.getGroupId(), userId)) {
                throw new RuntimeException("구독이 필요합니다.");
            }
        }
    }

    // IDOL 본인 판별 (board DB idols 기준)
    private boolean isMyIdol(Long targetIdolId, Integer userId) {
        Idol me = idolRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("아이돌 정보를 찾을 수 없습니다."));
        return me.getId() != null && targetIdolId != null && me.getId().longValue() == targetIdolId;
    }

    // 수정 권한 판단
    private boolean canModify(Comment comment, Integer userId, Role role) {
        if (role == Role.ADMIN)
            return true;
        return comment.getAuthorId().equals(userId);
    }

    // 삭제 권한 판단
    private boolean canDelete(Comment comment, Integer userId, Role role) {
        if (role == Role.ADMIN)
            return true;
        if (comment.getAuthorId().equals(userId))
            return true;

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

    // Comment -> CommentResponse 변환
    private CommentResponse toResponse(Comment c) {
        CommentResponse res = new CommentResponse();
        res.setCommentId(c.getCommentId());
        res.setAuthorId(c.getAuthorId());
        res.setAuthorNickname(c.getAuthorNickname()); // 닉네임 반환

        boolean deleted = Boolean.TRUE.equals(c.getIsDeleted());
        res.setIsDeleted(deleted);
        res.setContent(deleted ? "삭제된 댓글입니다" : c.getContent());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (c.getCreatedAt() != null)
            res.setCreatedAt(c.getCreatedAt().format(formatter));
        if (c.getUpdatedAt() != null)
            res.setUpdatedAt(c.getUpdatedAt().format(formatter));

        return res;
    }
}