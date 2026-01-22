package com.bit.docker.boardservice.service;

public enum Role {
    USER,
    IDOL,
    AGENCY,
    ADMIN;

    public static Role from(String raw) {
        if (raw == null) return null;
        return Role.valueOf(raw.trim().toUpperCase());
    }
}
