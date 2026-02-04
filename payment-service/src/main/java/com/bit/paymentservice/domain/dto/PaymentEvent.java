package com.bit.paymentservice.domain.dto;

import com.bit.paymentservice.domain.enumtype.PaymentDomain;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentEvent {
    private int userId;
    private String orderId;
    private PaymentDomain domain;
    private int targetId;
    private int amount;

    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PaymentEvent fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, PaymentEvent.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

