package com.bit.idol.notifyservice.kafka;

import com.bit.idol.notifyservice.repository.NotificationPreferenceRepository;
import com.bit.idol.notifyservice.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fanout 구조 기준:
 * - Notify-service는 "notify-fanout-topic"만 소비한다.
 * - fanout-service가 receiverId(단일 유저)를 채운 이벤트를 이 토픽으로 발행한다.
 * - 기존 chat/vote/ticket/notice 개별 토픽 소비는 fanout-service 쪽으로 옮기거나(권장),
 *   팀 합의에 따라 원본 서비스들이 모두 notify-request-topic으로 보내도록 통일한다.
 */
@Component
public class TopicConsumers {

    private final NotificationEventHandler handler;
    private final ObjectMapper om;
    private final NotificationRepository notificationRepo;
    private final NotificationPreferenceRepository prefRepo;

    public TopicConsumers(NotificationEventHandler handler,
                          ObjectMapper om,
                          NotificationRepository notificationRepo,
                          NotificationPreferenceRepository prefRepo) {
        this.handler = handler;
        this.om = om;
        this.notificationRepo = notificationRepo;
        this.prefRepo = prefRepo;
    }

    /**
     * ✅ Fanout 결과 토픽만 수신
     * - properties/yml에서 notify.topics.fanout 값을 관리하도록 한다.
     * - rawJson 그대로 handler에게 위임(기존 로직 유지)
     */
    @KafkaListener(topics = "${notify.topics.fanout}", groupId = "${notify.consumer.group-id:notify-fanout-group}")
    public void onFanoutEvent(String rawJson) {
        handler.handleNotification(rawJson);
    }

    /**
     * ✅ USER_DELETED 수신 시 Notify 데이터 정리 (기존 유지)
     * - 이 이벤트는 알림 저장 데이터 정리 목적이므로 fanout과 별개로 유지 가능
     */
    @KafkaListener(topics = "${notify.topics.user:user.events}", groupId = "${notify.consumer.user-group-id:notify-user-group}")
    @Transactional
    public void onUserEvent(String rawJson) {
        try {
            var root = om.readTree(rawJson);
            String eventType = root.path("eventType").asText("");
            if (!"USER_DELETED".equals(eventType)) return;

            int userId = root.path("data").path("userId").asInt(-1);
            if (userId <= 0) return;

            notificationRepo.deleteAllByReceiverId(userId);
            prefRepo.deleteByUserId(userId);

        } catch (Exception ignore) {
            // 기존 스타일 유지: 실패해도 전체 컨슈머 죽지 않게 무시
        }
    }
}
