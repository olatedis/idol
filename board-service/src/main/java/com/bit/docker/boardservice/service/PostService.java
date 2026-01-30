package com.bit.docker.boardservice.service;

import com.bit.docker.boardservice.client.SubscriptionInternalClient;
import com.bit.docker.boardservice.client.UserInternalClient;
import com.bit.docker.boardservice.dto.PostListResponse;
import com.bit.docker.boardservice.dto.PostResponse;
import com.bit.docker.boardservice.dto.PostUpdateRequest;
import com.bit.docker.boardservice.dto.PostWriteRequest;
import com.bit.docker.boardservice.dto.comment.CommentResponse;
import com.bit.docker.boardservice.entity.BoardType;
import com.bit.docker.boardservice.entity.Comment;
import com.bit.docker.boardservice.entity.Post;
import com.bit.docker.boardservice.kafka.NotifyProducer;
import com.bit.docker.boardservice.kafka.NotifyRequestEvent;
import com.bit.docker.boardservice.kafka.NotifyTargetType;
import com.bit.docker.boardservice.repository.CommentRepository;
import com.bit.docker.boardservice.repository.PostReactionRepository;
import com.bit.docker.boardservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    private final CommentRepository commentRepository;
    private final PostReactionRepository postReactionRepository;

    private final UserInternalClient userInternalClient;
    private final SubscriptionInternalClient subscriptionInternalClient;

    private final NotifyProducer notifyProducer;

    @Transactional
    public PostResponse insert(PostWriteRequest req, Integer userId, Role role) {
        validateBoardScope(req.getBoardType(), req.getIdolId(), req.getGroupId());

        requireCreatePermission(req.getBoardType(), req.getIdolId(), req.getGroupId(), userId, role);

        Post post = new Post();
        post.setBoardType(req.getBoardType());
        post.setIdolId(req.getIdolId());
        post.setGroupId(req.getGroupId());
        post.setAuthorId(userId);
        post.setTitle(requireNonBlank(req.getTitle()));
        post.setContent(requireNonBlank(req.getContent()));

        Post saved = postRepository.save(post);

        publishNewPostNotify(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PostResponse selectOne(Long postId, Integer userId, Role role) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // OFFICIAL/FAN 모두 상세보기(content)는 구독자만
        requireReadSubscription(post, userId, role);

        // 조회수 증가
        postRepository.increaseViewCount(postId);

        // 증가된 값 다시 반영
        post.setViewCount(post.getViewCount() + 1);

        return toResponse(post);
    }

    @Transactional(readOnly = true)
    public Page<PostListResponse> selectAll(BoardType boardType, Long idolId, Long groupId, Pageable pageable) {
        validateBoardScope(boardType, idolId, groupId);

        Page<Post> page;

        // IDOL_* 목록
        if (boardType == BoardType.IDOL_OFFICIAL || boardType == BoardType.IDOL_FAN) {
            page = postRepository.findByBoardTypeAndIdolIdOrderByCreatedAtDesc(boardType, idolId, pageable);
        }
        // GROUP_* 목록
        else if (boardType == BoardType.GROUP_OFFICIAL || boardType == BoardType.GROUP_FAN) {
            page = postRepository.findByBoardTypeAndGroupIdOrderByCreatedAtDesc(boardType, groupId, pageable);
        }
        // 그 외 케이스는 없음
        else {
            page = postRepository.findByBoardTypeOrderByCreatedAtDesc(boardType, pageable);
        }

        return page.map(this::toListResponse);
    }

    @Transactional
    public PostResponse update(Long postId, PostUpdateRequest req, Integer userId, Role role) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        requireUpdatePermission(post, userId, role);

        if (req.getTitle() != null) post.setTitle(requireNonBlank(req.getTitle()));
        if (req.getContent() != null) post.setContent(requireNonBlank(req.getContent()));

        Post saved = postRepository.save(post);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long postId, Integer userId, Role role) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        requireDeletePermission(post, userId, role);

        // Post 하드삭제 - 반응도 함께 하드삭제
        postReactionRepository.deleteByPost_PostId(postId);

        // Post 하드삭제 - 댓글도 함께 하드삭제
        commentRepository.deleteByPost_PostId(postId);

        postRepository.delete(post);
    }


    // Validation & Permission

    // 누구 소속 게시판인지확인하고 아이디 할당
    private void validateBoardScope(BoardType boardType, Long idolId, Long groupId) {
        if (boardType == null) throw new RuntimeException("게시판 타입은 필수입니다.");

        // IDOL_* : idolId 필수, groupId 금지
        if (boardType == BoardType.IDOL_OFFICIAL || boardType == BoardType.IDOL_FAN) {
            if (idolId == null) throw new RuntimeException("아이돌 게시판에는 아이돌 ID가 필수입니다.");
            if (groupId != null) throw new RuntimeException("아이돌 게시판에는 그룹 ID가 없어야 합니다.");
            return;
        }

        // GROUP_* : groupId 필수, idolId 금지
        if (boardType == BoardType.GROUP_OFFICIAL || boardType == BoardType.GROUP_FAN) {
            if (groupId == null) throw new RuntimeException("그룹 게시판에는 그룹 ID가 필수입니다.");
            if (idolId != null) throw new RuntimeException("그룹 게시판에는 아이돌 ID가 없어야 합니다.");
            return;
        }

        // enum 확장/오류 케이스 대비
        throw new RuntimeException("유효하지 않은 게시판 타입입니다.");
    }

    private void requireCreatePermission(BoardType boardType, Long idolId, Long groupId, Integer userId, Role role) {
        if (role == null) throw new RuntimeException("권한 정보가 필요합니다.");

        // ADMIN은 모든 것 가능
        if (role == Role.ADMIN) return;

        // IDOL_FAN: 팬(USER)만 작성 + 구독자만
        if (boardType == BoardType.IDOL_FAN) {
            if (role != Role.USER) throw new RuntimeException("접근 권한이 없습니다.");
            boolean ok = subscriptionInternalClient.isActiveIdolSubscriber(idolId, userId);
            if (!ok) throw new RuntimeException("구독이 필요합니다.");
            return;
        }

        // GROUP_FAN: 팬(USER)만 작성 + 구독자만
        if (boardType == BoardType.GROUP_FAN) {
            if (role != Role.USER) throw new RuntimeException("접근 권한이 없습니다.");
            boolean ok = subscriptionInternalClient.isActiveGroupSubscriber(groupId, userId);
            if (!ok) throw new RuntimeException("구독이 필요합니다.");
            return;
        }

        // IDOL_OFFICIAL: IDOL/AGENCY만 작성 가능
        if (boardType == BoardType.IDOL_OFFICIAL) {
            // IDOL/AGENCY만 작성 가능
            if (role == Role.IDOL) {
                boolean ok = userInternalClient.isIdolOwner(idolId, userId);
                if (!ok) throw new RuntimeException("접근 권한이 없습니다.");
                return;
            }
            if (role == Role.AGENCY) {
                boolean ok = userInternalClient.canAgencyManageIdol(userId, idolId);
                if (!ok) throw new RuntimeException("접근 권한이 없습니다.");
                return;
            }
            throw new RuntimeException("접근 권한이 없습니다.");
        }

        // GROUP_OFFICIAL: 그룹 멤버(IDOL) 또는 AGENCY 가능
        if (boardType == BoardType.GROUP_OFFICIAL) {
            // 그룹 게시판: 그룹 멤버 IDOL 또는 AGENCY 가능
            if (role == Role.IDOL) {
                boolean ok = userInternalClient.isGroupMember(groupId, userId);
                if (!ok) throw new RuntimeException("접근 권한이 없습니다.");
                return;
            }
            if (role == Role.AGENCY) {
                boolean ok = userInternalClient.canAgencyManageGroup(userId, groupId);
                if (!ok) throw new RuntimeException("접근 권한이 없습니다.");
                return;
            }
            throw new RuntimeException("접근 권한이 없습니다.");
        }

        throw new RuntimeException("접근 권한이 없습니다.");
    }

    private void requireUpdatePermission(Post post, Integer userId, Role role) {
        if (role == null) throw new RuntimeException("권한 정보가 필요합니다.");

        // ADMIN은 모든 것 가능
        if (role == Role.ADMIN) return;

        // FAN 게시판: USER는 본인 글만 수정 가능
        if (post.getBoardType() == BoardType.IDOL_FAN || post.getBoardType() == BoardType.GROUP_FAN) {
            if (role == Role.USER && post.getAuthorId().equals(userId)) return;
            throw new RuntimeException("접근 권한이 없습니다.");
        }

        // IDOL_OFFICIAL 수정: IDOL/AGENCY만
        if (post.getBoardType() == BoardType.IDOL_OFFICIAL) {
            // IDOL/GROUP 게시판 수정: IDOL/AGENCY만, 범위 검증 포함
            if (role == Role.IDOL) {
                boolean ok = userInternalClient.isIdolOwner(post.getIdolId(), userId);
                if (!ok) throw new RuntimeException("접근 권한이 없습니다.");
                return;
            }
            if (role == Role.AGENCY) {
                boolean ok = userInternalClient.canAgencyManageIdol(userId, post.getIdolId());
                if (!ok) throw new RuntimeException("접근 권한이 없습니다.");
                return;
            }
            throw new RuntimeException("접근 권한이 없습니다.");
        }

        // GROUP_OFFICIAL 수정: IDOL(멤버)/AGENCY만
        if (post.getBoardType() == BoardType.GROUP_OFFICIAL) {
            if (role == Role.IDOL) {
                boolean ok = userInternalClient.isGroupMember(post.getGroupId(), userId);
                if (!ok) throw new RuntimeException("접근 권한이 없습니다.");
                return;
            }
            if (role == Role.AGENCY) {
                boolean ok = userInternalClient.canAgencyManageGroup(userId, post.getGroupId());
                if (!ok) throw new RuntimeException("접근 권한이 없습니다.");
                return;
            }
            throw new RuntimeException("접근 권한이 없습니다.");
        }

        throw new RuntimeException("접근 권한이 없습니다.");
    }

    private void requireDeletePermission(Post post, Integer userId, Role role) {
        // 삭제 권한은 수정과 동일
        requireUpdatePermission(post, userId, role);
    }

    private void requireReadSubscription(Post post, Integer userId, Role role) {
        // ADMIN은 읽기제한 없음
        if (role == Role.ADMIN) return;

        // 개인이든 그룹이든 상세조회는 구독자만
        if (post.getBoardType() == BoardType.IDOL_OFFICIAL || post.getBoardType() == BoardType.IDOL_FAN) {
            boolean ok = subscriptionInternalClient.isActiveIdolSubscriber(post.getIdolId(), userId);
            if (!ok) throw new RuntimeException("구독이 필요합니다.");
            return;
        }

        if (post.getBoardType() == BoardType.GROUP_OFFICIAL || post.getBoardType() == BoardType.GROUP_FAN) {
            boolean ok = subscriptionInternalClient.isActiveGroupSubscriber(post.getGroupId(), userId);
            if (!ok) throw new RuntimeException("구독이 필요합니다.");
            return;
        }
    }

    // Notify

    private void publishNewPostNotify(Post post) {
        // FAN 게시판은 기본 알림 없음
        if (post.getBoardType() == BoardType.IDOL_FAN || post.getBoardType() == BoardType.GROUP_FAN) return;

        // OFFICIAL만 알림 발송
        if (post.getBoardType() != BoardType.IDOL_OFFICIAL && post.getBoardType() != BoardType.GROUP_OFFICIAL) return;

        NotifyRequestEvent event = new NotifyRequestEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setType("BOARD_NEW_POST");

        // IDOL_OFFICIAL -> IDOL_SUB
        if (post.getBoardType() == BoardType.IDOL_OFFICIAL) {
            event.setTargetType(NotifyTargetType.IDOL_SUB.name());
            event.setTargetId(String.valueOf(post.getIdolId()));
        }
        // GROUP_OFFICIAL -> GROUP_SUB
        else if (post.getBoardType() == BoardType.GROUP_OFFICIAL) {
            event.setTargetType(NotifyTargetType.GROUP_SUB.name());
            event.setTargetId(String.valueOf(post.getGroupId()));
        }

        Map<String, String> args = new HashMap<>();
        args.put("postId", String.valueOf(post.getPostId()));
        args.put("title", post.getTitle());
        args.put("boardType", post.getBoardType().name());
        if (post.getIdolId() != null) args.put("idolId", String.valueOf(post.getIdolId()));
        if (post.getGroupId() != null) args.put("groupId", String.valueOf(post.getGroupId()));
        event.setArgs(args);

        String redirectUrl;
        if (post.getBoardType() == BoardType.IDOL_OFFICIAL) {
            redirectUrl = "/board/posts/" + post.getPostId()
                    + "?boardType=IDOL_OFFICIAL&idolId=" + post.getIdolId();
        } else {
            redirectUrl = "/board/posts/" + post.getPostId()
                    + "?boardType=GROUP_OFFICIAL&groupId=" + post.getGroupId();
        }
        event.setRedirectUrl(redirectUrl);

        event.setOccurredAt(OffsetDateTime.now().toString());

        notifyProducer.send(event);
    }

    // Mapper
    // 엔티티(Post)를 api 응답용 dto로 바꿔주는 변환기

    // 상세 조회용
    private PostResponse toResponse(Post post) {
        PostResponse res = new PostResponse();
        // copyProperties:
        // post 안에 있는 필드 중 res에도 같은이름 + 같은 타입의 필드가 있으면 getter, setter이용해서 자동복사
        BeanUtils.copyProperties(post, res);

        // comments 포함(최신이 위)
        // isDeleted=true면 content는 "삭제된 댓글입니다"로 내려줌
        List<Comment> comments = commentRepository.findByPost_PostIdOrderByCreatedAtDesc(post.getPostId());
        List<CommentResponse> commentResponses = comments.stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
        res.setComments(commentResponses);

        return res;
    }

    // Comment 엔티티를 댓글dto로 변환, 소프트 댓글은 삭제된댓글로 치환
    private CommentResponse toCommentResponse(Comment c) {
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

    // 목록 조회용
    private PostListResponse toListResponse(Post post) {
        PostListResponse res = new PostListResponse();
        BeanUtils.copyProperties(post, res);
        return res;
    }

    private String requireNonBlank(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.isEmpty()) throw new RuntimeException("빈 문자열은 허용되지 않습니다.");
        return t;
    }
}
