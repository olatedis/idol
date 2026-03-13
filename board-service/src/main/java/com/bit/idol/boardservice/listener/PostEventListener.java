package com.bit.idol.boardservice.listener;

import com.bit.idol.boardservice.dto.event.PostCreatedEvent;
import com.bit.idol.boardservice.entity.BoardType;
import com.bit.idol.boardservice.entity.Post;
import com.bit.idol.boardservice.kafka.NotifyProducer;
import com.bit.idol.boardservice.kafka.NotifyRequestEvent;
import com.bit.idol.boardservice.kafka.NotifyTargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostEventListener {

    private final NotifyProducer notifyProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostCreated(PostCreatedEvent event) {
        Post post = event.post();
        log.info("게시글 알림 이벤트 처리 (After Commit): postId={}", post.getPostId());

        try {
            publishNewPostNotify(post);
        } catch (Exception e) {
            log.error("게시글 알림 발송 실패: {}", e.getMessage());
        }
    }

    private void publishNewPostNotify(Post post) {

        // ADMIN_NOTICE: 전체 공지 알림(ALL)
        if (post.getBoardType() == BoardType.ADMIN_NOTICE) {
            NotifyRequestEvent event = new NotifyRequestEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setType("BOARD_ADMIN_NOTICE");

            event.setTargetType(NotifyTargetType.ALL.name());
            event.setTargetId(null);

            Map<String, String> args = new HashMap<>();
            args.put("postId", String.valueOf(post.getPostId()));
            args.put("title", post.getTitle());
            args.put("boardType", post.getBoardType().name());
            event.setArgs(args);

            event.setRedirectUrl("/notices/" + post.getPostId());
            event.setOccurredAt(LocalDateTime.now().toString());

            notifyProducer.send(event);
            return;
        }

        // FAN 게시판은 기본 알림 없음 (IDOL_FAN 제거 → GROUP_FAN만)
        if (post.getBoardType() == BoardType.GROUP_FAN) return;

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
        else {
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
            // 수정: IDOL_OFFICIAL은 저장 시점에 groupId가 자동 세팅되어 있어야 함
            if (post.getGroupId() == null) {
                throw new RuntimeException("IDOL_OFFICIAL 게시글의 groupId가 없습니다.");
            }

            redirectUrl = "/group/" + post.getGroupId()
                    + "/idol/" + post.getIdolId()
                    + "/board/" + post.getPostId();
        } else {
            redirectUrl = "/group/" + post.getGroupId()
                    + "/board/" + post.getPostId();
        }
        event.setRedirectUrl(redirectUrl);

        event.setOccurredAt(LocalDateTime.now().toString());

        notifyProducer.send(event);
    }
}