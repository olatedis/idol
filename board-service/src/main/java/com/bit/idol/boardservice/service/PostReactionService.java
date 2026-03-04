package com.bit.idol.boardservice.service;

import com.bit.idol.boardservice.client.SubscriptionInternalClient;
import com.bit.idol.boardservice.dto.reaction.PostReactionResponse;
import com.bit.idol.boardservice.entity.BoardType;
import com.bit.idol.boardservice.entity.Idol;
import com.bit.idol.boardservice.entity.Post;
import com.bit.idol.boardservice.entity.PostReaction;
import com.bit.idol.boardservice.entity.ReactionType;
import com.bit.idol.boardservice.repository.IdolRepository;
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

    // IDOL 본인 체크용 (board DB idols)
    private final IdolRepository idolRepository;

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

    // 내부로직

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
                postRepository.incrementLikeCount(postId);
                post.setLikeCount(post.getLikeCount() + 1); // 객체 동기화
            } else {
                postRepository.incrementDislikeCount(postId);
                post.setDislikeCount(post.getDislikeCount() + 1); // 객체 동기화
            }

            return buildResponse(post, requested);
        }

        PostReaction existing = opt.get();

        // 2) 같은 반응 요청 → 삭제(토글 off)
        if (existing.getReactionType() == requested) {
            postReactionRepository.delete(existing);

            if (requested == ReactionType.LIKE) {
                postRepository.decrementLikeCount(postId);
                post.setLikeCount(post.getLikeCount() - 1); // 객체 동기화
            } else {
                postRepository.decrementDislikeCount(postId);
                post.setDislikeCount(post.getDislikeCount() - 1); // 객체 동기화
            }

            return buildResponse(post, "NONE");
        }

        // 3) 다른 반응 요청 → 타입 변경
        ReactionType before = existing.getReactionType();
        existing.setReactionType(requested);

        // 카운트 조정
        if (before == ReactionType.LIKE) {
            postRepository.decrementLikeCount(postId);
            post.setLikeCount(post.getLikeCount() - 1);
        } else {
            postRepository.decrementDislikeCount(postId);
            post.setDislikeCount(post.getDislikeCount() - 1);
        }

        if (requested == ReactionType.LIKE) {
            postRepository.incrementLikeCount(postId);
            post.setLikeCount(post.getLikeCount() + 1);
        } else {
            postRepository.incrementDislikeCount(postId);
            post.setDislikeCount(post.getDislikeCount() + 1);
        }

        return buildResponse(post, requested);
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
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

            boolean ok = subscriptionInternalClient.isActiveIdolSubscriber(post.getIdolId(), userId);
            if (!ok) throw new RuntimeException("구독이 필요합니다.");
            return;
        }

        // GROUP_OFFICIAL / GROUP_FAN
        if (post.getBoardType() == BoardType.GROUP_OFFICIAL || post.getBoardType() == BoardType.GROUP_FAN) {

            // GROUP_OFFICIAL은 IDOL/AGENCY 통과(정책)
            if (post.getBoardType() == BoardType.GROUP_OFFICIAL) {
                if (role == Role.IDOL || role == Role.AGENCY) return;
            }

            boolean ok = subscriptionInternalClient.isActiveGroupSubscriber(post.getGroupId(), userId);
            if (!ok) throw new RuntimeException("구독이 필요합니다.");
        }
    }

    //  IDOL 본인 판별 (board DB idols 기준)
    private boolean isMyIdol(Long targetIdolId, Integer userId) {
        Idol me = idolRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("아이돌 정보를 찾을 수 없습니다."));
        return me.getId() != null && targetIdolId != null && me.getId().longValue() == targetIdolId;
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