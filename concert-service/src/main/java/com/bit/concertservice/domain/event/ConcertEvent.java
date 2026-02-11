package com.bit.concertservice.domain.event;

import com.bit.concertservice.domain.entity.Concert;

public record ConcertEvent(String type, Concert concert) {
}
