package com.bit.concertservice.domain.entity;


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

    private int groupId;

    private String title;

    private String description;

    private String venue;

    private String img;

    private LocalDateTime concertDate;

    private LocalDateTime startTime;

    private LocalDateTime ticketSaleDate;

    private LocalDateTime createdAt;
    private boolean active = true;

    protected Concert() {
    }

    public Concert(int agencyId, int groupId, String title, String description, String venue, String img, LocalDateTime concertDate, LocalDateTime startTime, LocalDateTime ticketSaleDate) {
        this.agencyId = agencyId;
        this.groupId = groupId;
        this.title = title;
        this.description = description;
        this.venue = venue;
        this.img = img;
        this.concertDate = concertDate;
        this.startTime = startTime;
        this.ticketSaleDate = ticketSaleDate;
        this.createdAt = LocalDateTime.now();
    }

    public static Concert create(int agencyId, int groupId, String title, String description, String venue, String img, LocalDateTime concertDate, LocalDateTime startTime, LocalDateTime ticketSaleDate) {
        return new Concert(agencyId, groupId, title, description, venue, img, concertDate, startTime, ticketSaleDate);
    }

    public void update(String title, String description, String venue, String img, LocalDateTime concertDate, LocalDateTime startTime, LocalDateTime ticketSaleDate) {
        if (title != null && !title.isBlank()) this.title = title;
        if (description != null) this.description = description;
        if (venue != null && !venue.isBlank()) this.venue = venue;
        if (img != null) this.img = img;
        if (concertDate != null) this.concertDate = concertDate;
        if (startTime != null) this.startTime = startTime;
        if (ticketSaleDate != null) this.ticketSaleDate = ticketSaleDate;
    }

    public void deactivate() {
        this.active = false;
    }

}