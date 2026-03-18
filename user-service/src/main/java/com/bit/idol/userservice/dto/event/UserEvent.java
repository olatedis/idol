package com.bit.idol.userservice.dto.event;

public record UserEvent(int userId, String type, String status) {
}
