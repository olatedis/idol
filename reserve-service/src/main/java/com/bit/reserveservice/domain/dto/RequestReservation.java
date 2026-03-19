package com.bit.reserveservice.domain.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RequestReservation {
    private int userId;
    private int concertId;
    private int seatId;
    private java.util.List<Integer> seatIds;
    private int price;
}
