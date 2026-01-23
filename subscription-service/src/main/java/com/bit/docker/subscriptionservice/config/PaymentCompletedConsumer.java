package com.bit.docker.subscriptionservice.config;

import com.bit.docker.subscriptionservice.dto.PaymentEvent;
import com.bit.docker.subscriptionservice.entity.Subscription;
import com.bit.docker.subscriptionservice.repository.SubscriptionRepository;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedConsumer {

    private final Gson gson;
    private final SubscriptionRepository subscriptionRepository;

    @KafkaListener(
            topics = "payment.completed",
            groupId = "subscription-service"
    )
    public void handle(String message) {
        PaymentEvent event = gson.fromJson(message, PaymentEvent.class);

        Subscription subscription =
                subscriptionRepository
                        .findById(event.getTargetId())
                        .orElseThrow();

        subscription.activate();
    }
}
