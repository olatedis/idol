package com.bit.concertservice.domain.dto;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {
    private int userId;
    private String orderId;
    private String domain;
    private int targetId;
    private int amount;
    private List<Integer> reservationIds;
    private List<Integer> seatIds;


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
