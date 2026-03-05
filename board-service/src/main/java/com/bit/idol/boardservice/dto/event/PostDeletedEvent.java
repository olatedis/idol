package com.bit.idol.boardservice.dto.event;

import com.bit.idol.boardservice.entity.Post;

public record PostDeletedEvent(Post post) {
}