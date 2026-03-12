package com.bit.idol.boardservice.service;

import com.bit.idol.boardservice.client.SearchInternalClient;
import com.bit.idol.boardservice.client.SubscriptionInternalClient;
import com.bit.idol.boardservice.client.UserInternalClient;
import com.bit.idol.boardservice.dto.PostListResponse;
import com.bit.idol.boardservice.dto.PostResponse;
import com.bit.idol.boardservice.dto.PostUpdateRequest;
import com.bit.idol.boardservice.dto.PostWriteRequest;
import com.bit.idol.boardservice.dto.comment.CommentResponse;
import com.bit.idol.boardservice.dto.event.PostCreatedEvent;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bit.idol.boardservice.dto.search.PageResponse;
import com.bit.idol.boardservice.dto.search.PostSearchResponse;
import com.bit.idol.boardservice.entity.BoardType;
import com.bit.idol.boardservice.entity.Comment;
import com.bit.idol.boardservice.entity.Idol;
import com.bit.idol.boardservice.entity.Post;
import com.bit.idol.boardservice.repository.CommentRepository;
import com.bit.idol.boardservice.repository.IdolRepository;
import com.bit.idol.boardservice.repository.PostReactionRepository;
import com.bit.idol.boardservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostReactionRepository postReactionRepository;

    private final UserInternalClient userInternalClient;
    private final SubscriptionInternalClient subscriptionInternalClient;

    private final ApplicationEventPublisher eventPublisher;

    private final IdolRepository idolRepository;

    private final SearchInternalClient searchInternalClient;

    private final com.bit.idol.boardservice.kafka.PostIndexProducer postIndexProducer;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

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

        // 알림 이벤트(기존)
        eventPublisher.publishEvent(new PostCreatedEvent(saved));

        return toResponse(saved);
    }

    @Transactional
    public PostResponse selectOne(Long postId, Integer userId, Role role) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // OFFICIAL/FAN 모두 상세보기(content)는 구독자만 (단, IDOL_OFFICIAL은 IDOL/AGENCY 예외)
        requireReadSubscription(post, userId, role);

        postRepository.increaseViewCount(postId);
        post.setViewCount(post.getViewCount() + 1);

        return toResponse(post);
    }

    @Transactional(readOnly = true)
    public Page<PostListResponse> selectAll(
            BoardType boardType,
            Long idolId,
            Long groupId,
            String keyword,
            Pageable pageable
    ) {
        validateBoardScope(boardType, idolId, groupId);

        String k = (keyword == null) ? "" : keyword.trim();
        if (!k.isEmpty()) {
            int page = pageable.getPageNumber();
            int size = pageable.getPageSize();

            String sortParam = null;
            if (pageable.getSort() != null && pageable.getSort().isSorted()) {
                Sort.Order o = pageable.getSort().iterator().next();
                sortParam = o.getProperty() + "," + o.getDirection().name().toLowerCase();
            }

            PageResponse<PostSearchResponse> searchPage = searchInternalClient.searchPosts(
                    boardType.name(),
                    idolId,
                    groupId,
                    k,
                    page,
                    size,
                    sortParam
            );

            List<PostSearchResponse> searchContent = (searchPage.getContent() == null) ? List.of() : searchPage.getContent();
            List<Long> postIds = searchContent.stream()
                    .map(PostSearchResponse::getPostId)
                    .filter(Objects::nonNull)
                    .toList();

            if (postIds.isEmpty()) {
                return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, searchPage.getTotalElements());
            }

            List<Post> posts = postRepository.findByPostIdIn(postIds);

            Map<Long, Post> map = posts.stream()
                    .collect(Collectors.toMap(Post::getPostId, Function.identity(), (a, b) -> a));

            List<Post> ordered = postIds.stream()
                    .map(map::get)
                    .filter(Objects::nonNull)
                    .toList();

            List<PostListResponse> dtoList = ordered.stream()
                    .map(this::toListResponse)
                    .toList();

            return new org.springframework.data.domain.PageImpl<>(dtoList, pageable, searchPage.getTotalElements());
        }

        boolean likeSort = pageable.getSort().getOrderFor("likeCount") != null;

        Page<Post> page;

        if (boardType == BoardType.IDOL_OFFICIAL) {
            page = likeSort
                    ? postRepository.findByBoardTypeAndIdolIdOrderByLikeCountDesc(boardType, idolId, pageable)
                    : postRepository.findByBoardTypeAndIdolIdOrderByCreatedAtDesc(boardType, idolId, pageable);
        } else if (boardType == BoardType.GROUP_OFFICIAL || boardType == BoardType.GROUP_FAN) {
            page = likeSort
                    ? postRepository.findByBoardTypeAndGroupIdOrderByLikeCountDesc(boardType, groupId, pageable)
                    : postRepository.findByBoardTypeAndGroupIdOrderByCreatedAtDesc(boardType, groupId, pageable);
        } else {
            page = likeSort
                    ? postRepository.findByBoardTypeOrderByLikeCountDesc(boardType, pageable)
                    : postRepository.findByBoardTypeOrderByCreatedAtDesc(boardType, pageable);
        }

        return page.map(this::toListResponse);
    }

    @Transactional
    public PostResponse update(Long postId, PostUpdateRequest req, Integer userId, Role role) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        requireUpdatePermission(post, userId, role);

        if (req.getTitle() != null)
            post.setTitle(requireNonBlank(req.getTitle()));
        if (req.getContent() != null)
            post.setContent(requireNonBlank(req.getContent()));

        Post saved = postRepository.save(post);

        // 이벤트 발행 (AFTER_COMMIT에서 PostIndexEventPublisher가 Kafka로 보냄)
        eventPublisher.publishEvent(new com.bit.idol.boardservice.dto.event.PostUpdatedEvent(saved));

        return toResponse(saved);
    }

    @Transactional
    public void delete(Long postId, Integer userId, Role role) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        requireDeletePermission(post, userId, role);

        // 삭제 이벤트에 필요한 값만 스냅샷으로 확보
        Post snapshot = new Post();
        snapshot.setPostId(post.getPostId());
        snapshot.setBoardType(post.getBoardType());
        snapshot.setIdolId(post.getIdolId());
        snapshot.setGroupId(post.getGroupId());
        snapshot.setTitle(post.getTitle());
        snapshot.setContent(post.getContent());

        // Post 하드삭제 - 반응도 함께 하드삭제
        postReactionRepository.deleteByPost_PostId(postId);

        // Post 하드삭제 - 댓글도 함께 하드삭제
        commentRepository.deleteByPost_PostId(postId);

        postRepository.delete(post);

        // 이벤트 발행 (AFTER_COMMIT에서 PostIndexEventPublisher가 Kafka로 보냄)
        eventPublisher.publishEvent(new com.bit.idol.boardservice.dto.event.PostDeletedEvent(snapshot));
    }

    // Validation & Permission

    // 누구 소속 게시판인지확인하고 아이디 할당
    private void validateBoardScope(BoardType boardType, Long idolId, Long groupId) {
        if (boardType == null)
            throw new RuntimeException("게시판 타입은 필수입니다.");

        // ADMIN_NOTICE: idolId/groupId 둘 다 없어야 함
        if (boardType == BoardType.ADMIN_NOTICE) {
            if (idolId != null || groupId != null)
                throw new RuntimeException("공공지사항 게시판에는 idolId/groupId가 없어야 합니다.");
            return;
        }

        // IDOL_FAN 제거: IDOL_OFFICIAL만
        if (boardType == BoardType.IDOL_OFFICIAL) {
            if (idolId == null)
                throw new RuntimeException("아이돌 게시판에는 아이돌 ID가 필수입니다.");
            if (groupId != null)
                throw new RuntimeException("아이돌 게시판에는 그룹 ID가 없어야 합니다.");
            return;
        }

        // GROUP_* : groupId 필수, idolId 금지
        if (boardType == BoardType.GROUP_OFFICIAL || boardType == BoardType.GROUP_FAN) {
            if (groupId == null)
                throw new RuntimeException("그룹 게시판에는 그룹 ID가 필수입니다.");
            if (idolId != null)
                throw new RuntimeException("그룹 게시판에는 아이돌 ID가 없어야 합니다.");
            return;
        }

        throw new RuntimeException("유효하지 않은 게시판 타입입니다.");
    }

    private void requireCreatePermission(BoardType boardType, Long idolId, Long groupId, Integer userId, Role role) {
        if (role == null)
            throw new RuntimeException("권한 정보가 필요합니다.");

        // ADMIN_NOTICE: ADMIN만 작성 가능
        if (boardType == BoardType.ADMIN_NOTICE) {
            if (role != Role.ADMIN)
                throw new RuntimeException("접근 권한이 없습니다.");
            return;
        }

        // ADMIN은 모든 것 가능
        if (role == Role.ADMIN)
            return;

        // [수정] IDOL_FAN 제거됨

        // GROUP_FAN: 팬(USER)만 작성 + 구독자만
        if (boardType == BoardType.GROUP_FAN) {
            if (role != Role.USER)
                throw new RuntimeException("접근 권한이 없습니다.");
            boolean ok = subscriptionInternalClient.isActiveGroupSubscriber(groupId, userId);
            if (!ok)
                throw new RuntimeException("구독이 필요합니다.");
            return;
        }

        // IDOL_OFFICIAL: IDOL/AGENCY만 작성 가능
        if (boardType == BoardType.IDOL_OFFICIAL) {
            if (role == Role.IDOL) {
                //IDOL 본인 판별을 board DB idols 기준으로 통일
                if (!isMyIdol(idolId, userId))
                    throw new RuntimeException("접근 권한이 없습니다.");
                return;
            }
            if (role == Role.AGENCY) {
                boolean ok = userInternalClient.canAgencyManageIdol(userId, idolId);
                if (!ok)
                    throw new RuntimeException("접근 권한이 없습니다.");
                return;
            }
            throw new RuntimeException("접근 권한이 없습니다.");
        }

        // GROUP_OFFICIAL: 그룹 멤버(IDOL) 또는 AGENCY 가능
        if (boardType == BoardType.GROUP_OFFICIAL) {
            if (role == Role.IDOL) {
                boolean ok = userInternalClient.isGroupMember(groupId, userId);
                if (!ok)
                    throw new RuntimeException("접근 권한이 없습니다.");
                return;
            }
            if (role == Role.AGENCY) {
                boolean ok = userInternalClient.canAgencyManageGroup(userId, groupId);
                if (!ok)
                    throw new RuntimeException("접근 권한이 없습니다.");
                return;
            }
            throw new RuntimeException("접근 권한이 없습니다.");
        }

        throw new RuntimeException("접근 권한이 없습니다.");
    }

    private void requireUpdatePermission(Post post, Integer userId, Role role) {
        if (role == null)
            throw new RuntimeException("권한 정보가 필요합니다.");

        // ADMIN_NOTICE: ADMIN만 수정 가능
        if (post.getBoardType() == BoardType.ADMIN_NOTICE) {
            if (role != Role.ADMIN)
                throw new RuntimeException("접근 권한이 없습니다.");
            return;
        }

        // ADMIN은 모든 것 가능
        if (role == Role.ADMIN)
            return;

        // IDOL_FAN 제거: GROUP_FAN만
        if (post.getBoardType() == BoardType.GROUP_FAN) {
            if (role == Role.USER && post.getAuthorId().equals(userId))
                return;
            if (role == Role.AGENCY) {
                boolean ok = userInternalClient.canAgencyManageGroup(userId, post.getGroupId());
                if (ok) return;
            }
            if (role == Role.IDOL) {
                boolean ok = userInternalClient.isGroupMember(post.getGroupId(), userId);
                if (ok) return;
            }
            throw new RuntimeException("접근 권한이 없습니다.");
        }

        // IDOL_OFFICIAL 수정: IDOL/AGENCY만
        if (post.getBoardType() == BoardType.IDOL_OFFICIAL) {
            if (role == Role.IDOL) {
                // 본인 idol 글만 수정 가능
                if (!isMyIdol(post.getIdolId(), userId))
                    throw new RuntimeException("접근 권한이 없습니다.");
                return;
            }
            if (role == Role.AGENCY) {
                boolean ok = userInternalClient.canAgencyManageIdol(userId, post.getIdolId());
                if (!ok)
                    throw new RuntimeException("접근 권한이 없습니다.");
                return;
            }
            throw new RuntimeException("접근 권한이 없습니다.");
        }

        // GROUP_OFFICIAL 수정: IDOL(멤버)/AGENCY만
        if (post.getBoardType() == BoardType.GROUP_OFFICIAL) {
            if (role == Role.IDOL) {
                boolean ok = userInternalClient.isGroupMember(post.getGroupId(), userId);
                if (!ok)
                    throw new RuntimeException("접근 권한이 없습니다.");
                return;
            }
            if (role == Role.AGENCY) {
                boolean ok = userInternalClient.canAgencyManageGroup(userId, post.getGroupId());
                if (!ok)
                    throw new RuntimeException("접근 권한이 없습니다.");
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

        // 공지사항은 구독 없이 전체 공개
        if (post.getBoardType() == BoardType.ADMIN_NOTICE) return;

        // ADMIN은 읽기 제한 없음
        if (role == Role.ADMIN) return;

        // IDOL_OFFICIAL: USER만 구독 체크, IDOL(본인)/AGENCY는 통과
        if (post.getBoardType() == BoardType.IDOL_OFFICIAL) {

            // IDOL 본인인지 확인
            if (role == Role.IDOL && isMyIdol(post.getIdolId(), userId)) return;

            // 소속사(AGENCY) 관리 권한 확인
            if (role == Role.AGENCY && userInternalClient.canAgencyManageIdol(userId, post.getIdolId())) return;

            // USER는 구독자만
            boolean ok = subscriptionInternalClient.isActiveIdolSubscriber(post.getIdolId(), userId);
            if (!ok) throw new RuntimeException("구독이 필요합니다.");
            return;
        }

        // GROUP_OFFICIAL / GROUP_FAN
        if (post.getBoardType() == BoardType.GROUP_OFFICIAL || post.getBoardType() == BoardType.GROUP_FAN) {

            // 에이전시 관리 권한 확인
            if (role == Role.AGENCY && userInternalClient.canAgencyManageGroup(userId, post.getGroupId())) return;

            // 그룹 멤버(IDOL)인지 확인
            if (role == Role.IDOL && userInternalClient.isGroupMember(post.getGroupId(), userId)) return;

            // 구독 확인
            boolean ok = subscriptionInternalClient.isActiveGroupSubscriber(post.getGroupId(), userId);
            if (!ok) throw new RuntimeException("구독이 필요합니다.");
        }
    }

    // IDOL 본인 판별 유틸
    private boolean isMyIdol(Long targetIdolId, Integer userId) {
        Idol me = idolRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("아이돌 정보를 찾을 수 없습니다."));
        return me.getId() != null && targetIdolId != null && me.getId().longValue() == targetIdolId;
    }

    // Mapper

    // 상세 조회용
    private PostResponse toResponse(Post post) {
        PostResponse res = new PostResponse();
        BeanUtils.copyProperties(post, res);

        // comments 포함(최신이 위)
        List<Comment> comments = commentRepository.findByPost_PostIdOrderByCreatedAtDesc(post.getPostId());
        List<CommentResponse> commentResponses = comments.stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
        res.setComments(commentResponses);

        return res;
    }

    private CommentResponse toCommentResponse(Comment c) {
        CommentResponse res = new CommentResponse();
        res.setCommentId(c.getCommentId());
        res.setAuthorId(c.getAuthorId());

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

    private PostListResponse toListResponse(Post post) {
        PostListResponse res = new PostListResponse();
        BeanUtils.copyProperties(post, res);

        res.setCommentCount(post.getCommentCount());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (post.getCreatedAt() != null)
            res.setCreatedAt(post.getCreatedAt().format(formatter));

        return res;
    }

    private String requireNonBlank(String s) {
        if (s == null)
            throw new RuntimeException("빈 문자열은 허용되지 않습니다.");
        String t = s.trim();
        if (t.isEmpty())
            throw new RuntimeException("빈 문자열은 허용되지 않습니다.");
        return t;
    }
}