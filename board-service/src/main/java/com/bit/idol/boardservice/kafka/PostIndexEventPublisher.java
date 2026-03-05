package com.bit.idol.boardservice.kafka;

import com.bit.idol.boardservice.dto.event.PostCreatedEvent;
import com.bit.idol.boardservice.dto.event.PostDeletedEvent;
import com.bit.idol.boardservice.dto.event.PostUpdatedEvent;
import com.bit.idol.boardservice.entity.Post;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostIndexEventPublisher {

    private final PostIndexProducer postIndexProducer;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(PostCreatedEvent event) {
        log.info("PostCreatedEvent 수신: postId={}", safePostId(event.post()));
        sendUpsert(event.post(), "CREATE");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUpdated(PostUpdatedEvent event) {
        log.info("PostUpdatedEvent 수신: postId={}", safePostId(event.post()));
        sendUpsert(event.post(), "UPDATE");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeleted(PostDeletedEvent event) {
        log.info("PostDeletedEvent 수신: postId={}", safePostId(event.post()));
        sendDelete(event.post(), "DELETE");
    }

    private void sendUpsert(Post post, String reason) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("action", "UPSERT");
            payload.put("reason", reason);
            payload.put("postId", post.getPostId());
            payload.put("boardType", post.getBoardType() == null ? null : post.getBoardType().name());
            payload.put("idolId", post.getIdolId());
            payload.put("groupId", post.getGroupId());
            payload.put("title", post.getTitle());
            payload.put("content", post.getContent());

            String json = objectMapper.writeValueAsString(payload);

            postIndexProducer.send(json);
            log.info("게시글 색인(UPSERT) 이벤트 발행 완료: postId={}", post.getPostId());
        } catch (Exception e) {
            log.error("게시글 색인(UPSERT) 이벤트 발행 실패: postId={}", safePostId(post), e);
        }
    }

    private void sendDelete(Post post, String reason) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("action", "DELETE");
            payload.put("reason", reason);
            payload.put("postId", post.getPostId());

            String json = objectMapper.writeValueAsString(payload);

            postIndexProducer.send(json);
            log.info("게시글 색인(DELETE) 이벤트 발행 완료: postId={}", post.getPostId());
        } catch (Exception e) {
            log.error("게시글 색인(DELETE) 이벤트 발행 실패: postId={}", safePostId(post), e);
        }
    }

    private Long safePostId(Post post) {
        return post == null ? null : post.getPostId();
    }
}