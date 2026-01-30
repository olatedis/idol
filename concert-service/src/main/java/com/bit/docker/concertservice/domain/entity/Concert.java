package com.bit.docker.concertservice.domain.entity;


import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "concert")
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String title;

    private String venue;

    private LocalDateTime concertDate;

    protected Concert() {
    }

    public Concert(String title, String venue, LocalDateTime concertDate) {
        this.title = title;
        this.venue = venue;
        this.concertDate = concertDate;
    }
}