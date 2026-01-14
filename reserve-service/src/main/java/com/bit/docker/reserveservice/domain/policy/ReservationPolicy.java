package com.bit.docker.reserveservice.domain.policy;


import com.bit.docker.reserveservice.domain.entity.Reservation;
import com.bit.docker.reserveservice.domain.enumtype.ReservationStatus;

public class ReservationPolicy {

    public static void validateReservable(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("예약 가능한 상태가 아닙니다.");
        }
    }
}
