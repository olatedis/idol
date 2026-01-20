package com.bit.idol.fanoutservice.service;

import com.bit.idol.fanoutservice.client.UserServiceClient;
import com.bit.idol.fanoutservice.dto.*;
import com.bit.idol.fanoutservice.kafka.NotifyFanoutProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

// targetType/targetId 기준으로 USER 단위로 풀어서 notify-fanout-topic으로 보냄
@Slf4j
@Service
@RequiredArgsConstructor
public class FanoutService {

    private final UserServiceClient userServiceClient;
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
            // TODO: idolId(req.targetId)로 구독자 userId 목록을 가져오는 client 붙이기
            log.warn("IDOL_SUB 아직 미구현. eventId={}", req.getEventId());
            return;
        }

        if (req.getTargetType() == TargetType.GROUP_SUB) {
            // TODO: groupId(req.targetId)로 구독자 userId 목록을 가져오는 client 붙이기
            log.warn("GROUP_SUB 아직 미구현. eventId={}", req.getEventId());
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

        int sent = 0;
        for (UserDto u : users) {
            if (u == null || u.getUserId() == null) continue;
            sendToOneUser(req, String.valueOf(u.getUserId()));
            sent++;
        }

        log.info("fanout ALL 완료. eventId={} count={}", req.getEventId(), sent);
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

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
