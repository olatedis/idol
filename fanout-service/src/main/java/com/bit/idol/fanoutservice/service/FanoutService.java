package com.bit.idol.fanoutservice.service;

import com.bit.idol.fanoutservice.client.UserServiceClient;
import com.bit.idol.fanoutservice.dto.NotifyData;
import com.bit.idol.fanoutservice.dto.NotifyEvent;
import com.bit.idol.fanoutservice.dto.UserDto;
import com.bit.idol.fanoutservice.kafka.NotifyFanoutProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FanoutService {

    private final UserServiceClient userServiceClient;
    private final NotifyFanoutProducer producer;

    public void handle(NotifyEvent requestEvent) {
        NotifyData data = requestEvent.getData();
        Map<String, Object> attrs = (data != null) ? data.getAttributes() : null;

        String target = extractTarget(attrs);

        // target이 없으면 "단건(USER)"으로 취급(=receiverId가 들어왔다고 가정)
        if (target == null || target.isBlank() || "USER".equalsIgnoreCase(target)) {
            // 단건 알림은 receiverId가 반드시 있어야 함
            if (data == null || data.getReceiverId() == null) {
                log.warn("USER(target 없음)인데 receiverId가 없습니다. eventId={}", requestEvent.getEventId());
                return;
            }
            producer.send(requestEvent);
            return;
        }

        // 그룹/전체 fanout
        if ("ALL".equalsIgnoreCase(target)) {
            fanoutAllUsers(requestEvent);
            return;
        }

        if ("ARTIST_SUBSCRIBERS".equalsIgnoreCase(target)) {
            // TODO: 구독/채팅 서비스에서 artistId로 구독자 userId 리스트 조회 구현 필요
            // 예: attrs.get("artistId")로 값 받고, subscribe-service(or chat-service) client로 조회
            log.warn("ARTIST_SUBSCRIBERS target은 아직 미구현(TODO). eventId={}", requestEvent.getEventId());
            return;
        }

        if ("VOTE_PARTICIPANTS".equalsIgnoreCase(target)) {
            // TODO: 투표 서비스에서 voteId 기준 참여자/관심자 목록 조회
            log.warn("VOTE_PARTICIPANTS target은 아직 미구현(TODO). eventId={}", requestEvent.getEventId());
            return;
        }

        // 그 외 target은 추후 확장
        log.warn("알 수 없는 target={} eventId={}", target, requestEvent.getEventId());
    }

    private void fanoutAllUsers(NotifyEvent requestEvent) {
        List<UserDto> users;
        try {
            users = userServiceClient.getAllUsers();
        } catch (Exception e) {
            log.error("user-service 전체 유저 조회 실패. eventId={}", requestEvent.getEventId(), e);
            return;
        }

        if (users == null || users.isEmpty()) {
            log.info("전체 유저가 0명입니다. eventId={}", requestEvent.getEventId());
            return;
        }

        for (UserDto u : users) {
            if (u == null || u.getUserId() == null) continue;

            NotifyEvent fanoutEvent = cloneForReceiver(requestEvent, u.getUserId());
            producer.send(fanoutEvent);
        }

        log.info("fanout ALL 완료. eventId={} count={}", requestEvent.getEventId(), users.size());
    }

    private NotifyEvent cloneForReceiver(NotifyEvent original, Integer receiverId) {
        // 간단 복제(얕은 복사). data는 새로 만들고 receiverId만 바꿈.
        // fanout에서 clone하는이유 - 원본이벤트1개를 받아서 유저별 이벤트 n개로 만들어야 함

        NotifyEvent e = new NotifyEvent();
        e.setEventId(original.getEventId());
        e.setEventType(original.getEventType());
        e.setOccurredAt(original.getOccurredAt());
        e.setProducer(original.getProducer());

        NotifyData od = original.getData();
        NotifyData nd = new NotifyData();
        if (od != null) {
            nd.setCategory(od.getCategory());
            nd.setTitle(od.getTitle());
            nd.setBody(od.getBody());
            nd.setDeeplink(od.getDeeplink());
            nd.setAttributes(od.getAttributes()); // target 정보 포함
        }
        nd.setReceiverId(receiverId);

        e.setData(nd);
        return e;
    }

    private String extractTarget(Map<String, Object> attrs) {
        if (attrs == null) return null;
        Object v = attrs.get("target"); // 합의: attributes.target 사용
        return (v == null) ? null : String.valueOf(v);
    }
}
