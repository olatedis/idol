package com.bit.subscriptionservice.dto;

import com.bit.subscriptionservice.enumtype.PaymentDomain;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentEvent {
    private int userId;
    private String orderId;
    private PaymentDomain domain;
    private int targetId;
    private int amount;
    private int agencyId;

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
