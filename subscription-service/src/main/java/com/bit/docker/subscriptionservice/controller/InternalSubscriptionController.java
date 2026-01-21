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
    public ResponseEntity<List<Integer>> getIdolSubscriberUserIds(@PathVariable("idolId") Long idolId) {
        return ResponseEntity.ok(subscriptionService.getActiveSubscriberUserIdsByIdolId(idolId));
    }

    // fanout용: 그룹 구독자 userId 리스트
    @GetMapping("/groups/{groupId}/user-ids")
    public ResponseEntity<List<Integer>> getGroupSubscriberUserIds(@PathVariable("groupId") Long groupId) {
        return ResponseEntity.ok(subscriptionService.getActiveSubscriberUserIdsByGroupId(groupId));
    }
}
