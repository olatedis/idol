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

    private int agencyId;

    private String title;

    private String description;

    private String venue;

    private LocalDateTime concertDate;

    private LocalDateTime startTime;

    private LocalDateTime ticketSaleDate;

    private LocalDateTime createdAt;
    private boolean active = true;

    protected Concert() {
    }

    public Concert(int agencyId, String title, String description, String venue, LocalDateTime concertDate, LocalDateTime startTime, LocalDateTime ticketSaleDate) {
        this.agencyId = agencyId;
        this.title = title;
        this.description = description;
        this.venue = venue;
        this.concertDate = concertDate;
        this.startTime = startTime;
        this.ticketSaleDate = ticketSaleDate;
        this.createdAt = LocalDateTime.now();
    }

    public static Concert create(int agencyId, String title, String description, String venue, LocalDateTime concertDate, LocalDateTime startTime, LocalDateTime ticketSaleDate) {
        return new Concert(agencyId, title, description, venue, concertDate, startTime, ticketSaleDate);
    }

    public void update(String title, String description, String venue, LocalDateTime concertDate, LocalDateTime startTime, LocalDateTime ticketSaleDate) {
        if (title != null && !title.isBlank()) this.title = title;
        if (description != null) this.description = description;
        if (venue != null && !venue.isBlank()) this.venue = venue;
        if (concertDate != null) this.concertDate = concertDate;
        if (startTime != null) this.startTime = startTime;
        if (ticketSaleDate != null) this.ticketSaleDate = ticketSaleDate;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isActive() { return this.active; }
}