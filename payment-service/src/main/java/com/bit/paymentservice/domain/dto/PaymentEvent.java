package com.bit.paymentservice.domain.dto;

import com.bit.paymentservice.domain.enumtype.PaymentDomain;
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
    private PaymentDomain domain;
    private int targetId;
    private int amount;
    private int agencyId;
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
            ObjectMapper mapper = new ObjectMapper();
            // 직접 역직렬화 시도
            return mapper.readValue(json, PaymentEvent.class);
        } catch (Exception e) {
            try {
                // Kafka StringSerializer가 문자열을 한 번 더 감싼 경우 (이중 직렬화) 처리
                // ex) "\"{ ... }\"" → "{ ... }" → PaymentEvent
                ObjectMapper mapper = new ObjectMapper();
                String unwrapped = mapper.readValue(json, String.class);
                return mapper.readValue(unwrapped, PaymentEvent.class);
            } catch (Exception e2) {
                throw new RuntimeException("PaymentEvent 역직렬화 실패: " + e2.getMessage(), e2);
            }
        }
    }
}
