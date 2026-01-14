package com.bit.idol.notifyservice.kafka;

import com.bit.idol.notifyservice.repository.NotificationPreferenceRepository;
import com.bit.idol.notifyservice.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @KafkaListener(topics = "chat.events", groupId = "notify-chat-group")
    public void onChatEvent(String rawJson) { handler.handleNotification(rawJson); }

    @KafkaListener(topics = "vote.events", groupId = "notify-vote-group")
    public void onVoteEvent(String rawJson) { handler.handleNotification(rawJson); }

    @KafkaListener(topics = "ticket.events", groupId = "notify-ticket-group")
    public void onTicketEvent(String rawJson) { handler.handleNotification(rawJson); }

    @KafkaListener(topics = "notice.events", groupId = "notify-notice-group")
    public void onNoticeEvent(String rawJson) { handler.handleNotification(rawJson); }

    // USER_DELETED 수신 시 Notify 데이터 정리
    @KafkaListener(topics = "user.events", groupId = "notify-user-group")
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
        }
    }
}
