package com.bit.subscriptionservice.dto.event;

import com.bit.subscriptionservice.dto.SubscriptionEvent;

public record SubscriptionEventWrapper(String topic, SubscriptionEvent event) {
}
