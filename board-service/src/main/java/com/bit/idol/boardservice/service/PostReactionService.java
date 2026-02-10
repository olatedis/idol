package com.bit.idol.boardservice.service;

import com.bit.idol.boardservice.client.SubscriptionInternalClient;
import com.bit.idol.boardservice.dto.reaction.PostReactionResponse;
import com.bit.idol.boardservice.entity.*;
import com.bit.idol.boardservice.entity.BoardType;
import com.bit.idol.boardservice.entity.Post;
import com.bit.idol.boardservice.entity.PostReaction;
import com.bit.idol.boardservice.entity.ReactionType;
import com.bit.idol.boardservice.repository.PostReactionRepository;
import com.bit.idol.boardservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostReactionService {

    private final PostRepository postRepository;
    private final PostReactionRepository postReactionRepository;

    private final SubscriptionInternalClient subscriptionInternalClient;

    @Transactional
    public PostReactionResponse like(Long postId, Integer userId, Role role) {
        return toggle(postId, userId, role, ReactionType.LIKE);
    }

    @Transactional
    public PostReactionResponse dislike(Long postId, Integer userId, Role role) {
        return toggle(postId, userId, role, ReactionType.DISLIKE);
    }


    @Transactional(readOnly = true)
    public PostReactionResponse getMyReaction(Long postId, Integer userId, Role role) {
        Post post = getPost(postId);
        requireReadSubscription(post, userId, role);

        Optional<PostReaction> opt = postReactionRepository.findByPost_PostIdAndUserId(postId, userId);

        PostReactionResponse res = new PostReactionResponse();
        res.setLikeCount(post.getLikeCount());
        res.setDislikeCount(post.getDislikeCount());
        res.setMyReaction(opt.map(r -> r.getReactionType().name()).orElse("NONE"));
        return res;
    }

    // 내부로직================

    // 토글처리
    private PostReactionResponse toggle(Long postId, Integer userId, Role role, ReactionType requested) {
        Post post = getPost(postId);
        requireReadSubscription(post, userId, role);

        Optional<PostReaction> opt = postReactionRepository.findByPost_PostIdAndUserId(postId, userId);

        // 1) 기존 반응 없음 → 생성
        if (opt.isEmpty()) {
            PostReaction pr = new PostReaction();
            pr.setPost(post);
            pr.setUserId(userId);
            pr.setReactionType(requested);

            postReactionRepository.save(pr);

            if (requested == ReactionType.LIKE) {
                post.setLikeCount(post.getLikeCount() + 1);
            } else {
                post.setDislikeCount(post.getDislikeCount() + 1);
            }

            return buildResponse(post, requested);
        }

        PostReaction existing = opt.get();

        // 2) 같은 반응 요청 → 삭제(토글 off)
        if (existing.getReactionType() == requested) {
            postReactionRepository.delete(existing);

            if (requested == ReactionType.LIKE) {
                post.setLikeCount(post.getLikeCount() - 1);
            } else {
                post.setDislikeCount(post.getDislikeCount() - 1);
            }

            return buildResponse(post, "NONE");
        }

        // 3) 다른 반응 요청 → 타입 변경
        ReactionType before = existing.getReactionType();
        existing.setReactionType(requested);

        // 카운트 조정
        if (before == ReactionType.LIKE) {
            post.setLikeCount(post.getLikeCount() - 1);
        } else {
            post.setDislikeCount(post.getDislikeCount() - 1);
        }

        if (requested == ReactionType.LIKE) {
            post.setLikeCount(post.getLikeCount() + 1);
        } else {
            post.setDislikeCount(post.getDislikeCount() + 1);
        }

        return buildResponse(post, requested);
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
    }

    // 게시글 상세 조회 권한(추천/비추천도 동일하게 적용)

    private void requireReadSubscription(Post post, Integer userId, Role role) {
        if (role == Role.ADMIN) return;

        if (post.getBoardType() == BoardType.IDOL_OFFICIAL || post.getBoardType() == BoardType.IDOL_FAN) {
            boolean ok = subscriptionInternalClient.isActiveIdolSubscriber(post.getIdolId(), userId);
            if (!ok) throw new RuntimeException("구독이 필요합니다.");
            return;
        }

        if (post.getBoardType() == BoardType.GROUP_OFFICIAL || post.getBoardType() == BoardType.GROUP_FAN) {
            boolean ok = subscriptionInternalClient.isActiveGroupSubscriber(post.getGroupId(), userId);
            if (!ok) throw new RuntimeException("구독이 필요합니다.");
        }
    }

    // 응답 구성
    private PostReactionResponse buildResponse(Post post, ReactionType my) {
        return buildResponse(post, my.name());
    }

    private PostReactionResponse buildResponse(Post post, String my) {
        PostReactionResponse res = new PostReactionResponse();
        res.setLikeCount(post.getLikeCount());
        res.setDislikeCount(post.getDislikeCount());
        res.setMyReaction(my);
        return res;
    }
}
