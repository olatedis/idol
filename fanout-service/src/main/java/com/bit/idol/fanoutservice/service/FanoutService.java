package com.bit.idol.fanoutservice.service;

import com.bit.idol.fanoutservice.client.SubscriptionServiceClient;
import com.bit.idol.fanoutservice.client.UserServiceClient;
import com.bit.idol.fanoutservice.dto.*;
import com.bit.idol.fanoutservice.kafka.NotifyFanoutProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// targetType/targetId 기준으로 USER 단위로 풀어서 notify-fanout-topic으로 보냄
@Slf4j
@Service
@RequiredArgsConstructor
public class FanoutService {

    private final UserServiceClient userServiceClient;
    private final SubscriptionServiceClient subscriptionServiceClient;
    private final NotifyFanoutProducer producer;

    @Value("${fanout.topics.fanout}")
    private String fanoutTopic;

    public void handle(NotifyRequestEvent req) {
        if (req == null) return;
        if (blank(req.getEventId()) || blank(req.getType()) || req.getTargetType() == null || blank(req.getRedirectUrl()) || blank(req.getOccurredAt())) {
            return;
        }

        if (req.getTargetType() == TargetType.USER) {
            if (blank(req.getTargetId())) return;
            sendToOneUser(req, req.getTargetId());
            return;
        }

        if (req.getTargetType() == TargetType.ALL) {
            fanoutAll(req);
            return;
        }

        if (req.getTargetType() == TargetType.IDOL_SUB) {
            fanoutIdolSubscribers(req);
            return;
        }

        if (req.getTargetType() == TargetType.GROUP_SUB) {
            fanoutGroupSubscribers(req);
            return;
        }

        log.warn("알 수 없는 targetType={}. eventId={}", req.getTargetType(), req.getEventId());
    }

    private void fanoutAll(NotifyRequestEvent req) {
        List<UserDto> users;
        try {
            users = userServiceClient.getAllUsers();
        } catch (Exception e) {
            log.error("user-service 전체 유저 조회 실패. eventId={}", req.getEventId(), e);
            return;
        }

        if (users == null || users.isEmpty()) {
            log.info("전체 유저 0명. eventId={}", req.getEventId());
            return;
        }

        Integer actorId = null;
        try {
            if (req.getArgs() != null && req.getArgs().get("actorId") !=null) {
                actorId = Integer.parseInt(req.getArgs().get("actorId"));
            }
        } catch (Exception e) {
            log.warn("actorId 파싱 실패. actorId={} eventId={}",
                    req.getArgs() !=null ? req.getArgs().get("actorId") : null,
                    req.getEventId());
        }

        int sent = 0;
        for (UserDto u : users) {
            if (u == null || u.getUserId() == null) continue;

            if (actorId != null && actorId.equals(u.getUserId())) continue;;
            sendToOneUser(req, String.valueOf(u.getUserId()));
            sent++;
        }

        log.info("fanout ALL 완료. eventId={} count={}", req.getEventId(), sent);
    }

    private void fanoutIdolSubscribers(NotifyRequestEvent req) {
        // req.targetId = idolId(개인 아이돌)
        if (blank(req.getTargetId())) {
            log.warn("IDOL_SUB인데 targetId(idolId)가 없습니다. eventId={}", req.getEventId());
            return;
        }

        Long idolId;
        try {
            idolId = Long.parseLong(req.getTargetId());
        } catch (Exception e) {
            log.warn("IDOL_SUB targetId 파싱 실패. targetId={} eventId={}", req.getTargetId(), req.getEventId());
            return;
        }

        List<Integer> userIds;
        try {
            userIds = subscriptionServiceClient.getIdolSubscriberUserIds(idolId);
        } catch (Exception e) {
            log.error("subscription-service idol 구독자 조회 실패. idolId={} eventId={}", idolId, req.getEventId(), e);
            return;
        }

        if (userIds == null || userIds.isEmpty()) {
            log.info("IDOL_SUB 구독자 0명. idolId={} eventId={}", idolId, req.getEventId());
            return;
        }

        int sent = 0;
        for (Integer uid : userIds) {
            if (uid == null || uid <= 0) continue;

            // IDOL_MESSAGE 스택 계산을 위해 idolId를 fanout 이벤트 args에 함께 전달
            sendToOneUserWithIdolId(req, String.valueOf(uid), String.valueOf(idolId));
            sent++;
        }
        log.info("fanout IDOL_SUB 완료. eventId={} idolId={} count={}", req.getEventId(), idolId, sent);
    }

    private void fanoutGroupSubscribers(NotifyRequestEvent req) {
        // req.targetId = groupId(그룹: 트와이스 등)
        if (blank(req.getTargetId())) {
            log.warn("GROUP_SUB인데 targetId(groupId)가 없습니다. eventId={}", req.getEventId());
            return;
        }

        Long groupId;
        try {
            groupId = Long.parseLong(req.getTargetId());
        } catch (Exception e) {
            log.warn("GROUP_SUB targetId 파싱 실패. targetId={} eventId={}", req.getTargetId(), req.getEventId());
            return;
        }

        List<Integer> userIds;
        try {
            userIds = subscriptionServiceClient.getGroupSubscriberUserIds(groupId);
        } catch (Exception e) {
            log.error("subscription-service group 구독자 조회 실패. groupId={} eventId={}", groupId, req.getEventId(), e);
            return;
        }

        if (userIds == null || userIds.isEmpty()) {
            log.info("GROUP_SUB 구독자 0명. groupId={} eventId={}", groupId, req.getEventId());
            return;
        }

        int sent = 0;
        for (Integer uid : userIds) {
            if (uid == null || uid <= 0) continue;
            sendToOneUser(req, String.valueOf(uid));
            sent++;
        }

        log.info("fanout GROUP_SUB 완료. eventId={} groupId={} count={}", req.getEventId(), groupId, sent);
    }

    private void sendToOneUser(NotifyRequestEvent req, String userIdStr) {
        NotifyFanoutEvent out = new NotifyFanoutEvent();

        // 유저별로 eventId를 바꿔야 notify-service UNIQUE(event_id)에서 충돌 안 남
        out.setEventId(req.getEventId() + ":" + userIdStr);

        out.setType(req.getType());
        out.setTargetType(TargetType.USER);
        out.setTargetId(userIdStr);
        out.setArgs(req.getArgs());
        out.setRedirectUrl(req.getRedirectUrl());
        out.setOccurredAt(req.getOccurredAt());

        producer.send(fanoutTopic, out);
    }

    private void sendToOneUserWithIdolId(NotifyRequestEvent req, String userIdStr, String idolIdStr) {
        NotifyFanoutEvent out = new NotifyFanoutEvent();

        // 유저별로 eventId를 바꿔야 notify-service UNIQUE(event_id)에서 충돌 안 남
        out.setEventId(req.getEventId() + ":" + userIdStr);

        out.setType(req.getType());
        out.setTargetType(TargetType.USER);
        out.setTargetId(userIdStr);

        // args 복사 후 idolId 주입
        // (원본 targetId(idolId)가 fanout 이후에는 사라지므로 notify-service까지 전달 목적)
        Map<String, String> args = (req.getArgs() == null) ? new HashMap<>() : new HashMap<>(req.getArgs());
        args.put("idolId", idolIdStr);

        out.setArgs(args);

        out.setRedirectUrl(req.getRedirectUrl());
        out.setOccurredAt(req.getOccurredAt());

        producer.send(fanoutTopic, out);
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
