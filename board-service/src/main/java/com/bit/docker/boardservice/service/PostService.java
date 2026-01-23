package com.bit.docker.boardservice.service;

import com.bit.docker.boardservice.client.*;
import com.bit.docker.boardservice.dto.PostListResponse;
import com.bit.docker.boardservice.dto.PostResponse;
import com.bit.docker.boardservice.dto.PostUpdateRequest;
import com.bit.docker.boardservice.dto.PostWriteRequest;
import com.bit.docker.boardservice.entity.BoardType;
import com.bit.docker.boardservice.entity.Post;
import com.bit.docker.boardservice.exception.ApiException;
import com.bit.docker.boardservice.kafka.NotifyProducer;
import com.bit.docker.boardservice.kafka.NotifyRequestEvent;
import com.bit.docker.boardservice.kafka.NotifyTargetType;
import com.bit.docker.boardservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

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
        post.setTitle(req.getTitle());
        post.setContent(req.getContent());

        Post saved = postRepository.save(post);

        publishNewPostNotify(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PostResponse selectOne(Long postId, Integer userId, Role role) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(404, "post not found"));

        // A안: IDOL 게시판은 "상세(content)"는 구독자만 (USER만 제한)
        if (post.getBoardType() == BoardType.IDOL && role == Role.USER) {
            boolean ok = subscriptionInternalClient.isActiveIdolSubscriber(post.getIdolId(), userId);
            if (!ok) throw new ApiException(403, "subscription required");
        }

        return toResponse(post);
    }


    @Transactional(readOnly = true)
    public Page<PostListResponse> selectAll(BoardType boardType, Long idolId, Long groupId, Pageable pageable) {
        validateBoardScope(boardType, idolId, groupId);

        Page<Post> page;
        if (boardType == BoardType.IDOL) {
            page = postRepository.findByBoardTypeAndIdolIdOrderByCreatedAtDesc(boardType, idolId, pageable);
        } else if (boardType == BoardType.GROUP) {
            page = postRepository.findByBoardTypeAndGroupIdOrderByCreatedAtDesc(boardType, groupId, pageable);
        } else {
            page = postRepository.findByBoardTypeOrderByCreatedAtDesc(boardType, pageable);
        }
        return page.map(this::toListResponse);
    }


    @Transactional
    public PostResponse update(Long postId, PostUpdateRequest req, Integer userId, Role role) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(404, "post not found"));

        requireUpdatePermission(post, userId, role);

        if (req.getTitle() != null) post.setTitle(nz(req.getTitle()));
        if (req.getContent() != null) post.setContent(nz(req.getContent()));

        Post saved = postRepository.save(post);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long postId, Integer userId, Role role) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(404, "post not found"));

        requireDeletePermission(post, userId, role);

        postRepository.delete(post);
    }

    // Validation & Permission

    private void validateBoardScope(BoardType boardType, Long idolId, Long groupId) {
        if (boardType == null) throw new ApiException(400, "boardType is required");

        if (boardType == BoardType.IDOL) {
            if (idolId == null) throw new ApiException(400, "idolId is required for IDOL board");
            if (groupId != null) throw new ApiException(400, "groupId must be null for IDOL board");
        } else if (boardType == BoardType.GROUP) {
            if (groupId == null) throw new ApiException(400, "groupId is required for GROUP board");
            if (idolId != null) throw new ApiException(400, "idolId must be null for GROUP board");
        } else { // FAN
            if (idolId != null || groupId != null) {
                throw new ApiException(400, "idolId/groupId must be null for FAN board");
            }
        }
    }

    private void requireCreatePermission(BoardType boardType, Long idolId, Long groupId, Integer userId, Role role) {
        if (boardType == BoardType.IDOL) {
            if (role == Role.IDOL) {
                boolean ok = userInternalClient.isIdolOwner(idolId, userId);
                if (!ok) throw new ApiException(403, "forbidden");
                return;
            }
            if (role == Role.AGENCY) {
                boolean ok = userInternalClient.canAgencyManageIdol(userId, idolId);
                if (!ok) throw new ApiException(403, "forbidden");
                return;
            }
            throw new ApiException(403, "forbidden");
        }

        if (boardType == BoardType.GROUP) {
            if (role == Role.IDOL) {
                boolean ok = userInternalClient.isGroupMember(groupId, userId);
                if (!ok) throw new ApiException(403, "forbidden");
                return;
            }
            if (role == Role.AGENCY) {
                boolean ok = userInternalClient.canAgencyManageGroup(userId, groupId);
                if (!ok) throw new ApiException(403, "forbidden");
                return;
            }
            throw new ApiException(403, "forbidden");
        }

    }

    private void requireUpdatePermission(Post post, Integer userId, Role role) {
        if (post.getBoardType() == BoardType.IDOL) {
            if (role == Role.IDOL) {
                boolean ok = userInternalClient.isIdolOwner(post.getIdolId(), userId);
                if (!ok) throw new ApiException(403, "forbidden");
                return;
            }
            if (role == Role.AGENCY) {
                boolean ok = userInternalClient.canAgencyManageIdol(userId, post.getIdolId());
                if (!ok) throw new ApiException(403, "forbidden");
                return;
            }
            throw new ApiException(403, "forbidden");
        }

        if (post.getBoardType() == BoardType.GROUP) {
            if (role == Role.IDOL) {
                boolean ok = userInternalClient.isGroupMember(post.getGroupId(), userId);
                if (!ok) throw new ApiException(403, "forbidden");
                return;
            }
            if (role == Role.AGENCY) {
                boolean ok = userInternalClient.canAgencyManageGroup(userId, post.getGroupId());
                if (!ok) throw new ApiException(403, "forbidden");
                return;
            }
            throw new ApiException(403, "forbidden");
        }

    }

    private void requireDeletePermission(Post post, Integer userId, Role role) {
        // 삭제 정책은 수정과 동일하게 두는 게 일반적
        requireUpdatePermission(post, userId, role);
    }

    // Notify

    private void publishNewPostNotify(Post post) {
        // FAN 게시판은 기본 “구독 알림” 없음
        if (post.getBoardType() == BoardType.FAN) return;

        NotifyRequestEvent event = new NotifyRequestEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setType("BOARD_NEW_POST");

        if (post.getBoardType() == BoardType.IDOL) {
            event.setTargetType(NotifyTargetType.IDOL_SUB.name());
            event.setTargetId(String.valueOf(post.getIdolId()));
        } else if (post.getBoardType() == BoardType.GROUP) {
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
        if (post.getBoardType() == BoardType.IDOL) {
            redirectUrl = "/board/posts/" + post.getPostId()
                    + "?boardType=IDOL&idolId=" + post.getIdolId();
        } else {
            redirectUrl = "/board/posts/" + post.getPostId()
                    + "?boardType=GROUP&groupId=" + post.getGroupId();
        }
        event.setRedirectUrl(redirectUrl);

        event.setOccurredAt(OffsetDateTime.now().toString());

        notifyProducer.send(event);
    }

    // Mapper

    private PostResponse toResponse(Post post) {
        PostResponse res = new PostResponse();
        BeanUtils.copyProperties(post, res);
        return res;
    }

    private String nz(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.isEmpty()) throw new ApiException(400, "empty string not allowed");
        return t;
    }

    private PostListResponse toListResponse(Post post) {
        PostListResponse res = new PostListResponse();
        BeanUtils.copyProperties(post, res);
        return res;
    }

}
