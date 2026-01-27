package com.bit.docker.subscriptionservice.controller;

import com.bit.docker.subscriptionservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/subscriptions")
@RequiredArgsConstructor
public class InternalSubscriptionController {

    private final SubscriptionService subscriptionService;

    // fanout용: 개인(아이돌) 구독자 userId 리스트
    @GetMapping("/idols/{idolId}/user-ids")
    public ResponseEntity<List<Integer>> getIdolSubscriberUserIds(@PathVariable("idolId") int idolId) {
        return ResponseEntity.ok(subscriptionService.getActiveSubscriberUserIdsByIdolId(idolId));
    }

    // fanout용: 그룹 구독자 userId 리스트
    @GetMapping("/groups/{groupId}/user-ids")
    public ResponseEntity<List<Integer>> getGroupSubscriberUserIds(@PathVariable("groupId") int groupId) {
        return ResponseEntity.ok(subscriptionService.getActiveSubscriberUserIdsByGroupId(groupId));
    }

    // idol 게시판 상세 열람용: userId가 idolId 유료구독(active) 중인지
    @GetMapping("/idols/{idolId}/users/{userId}/active")
    public ResponseEntity<Boolean> isActiveIdolSubscriber(
            @PathVariable("idolId") int idolId,
            @PathVariable("userId") int userId
    ) {
        return ResponseEntity.ok(subscriptionService.isActiveIdolSubscriber(userId, idolId));
    }

    // group 게시판 상세 열람용: userId가 groupId 구독(active) 중인지
    @GetMapping("/groups/{groupId}/users/{userId}/active")
    public ResponseEntity<Boolean> isActiveGroupSubscriber(
            @PathVariable("groupId") int groupId,
            @PathVariable("userId") int userId
    ) {
        return ResponseEntity.ok(subscriptionService.isActiveGroupSubscriber(userId, groupId));
    }
}
