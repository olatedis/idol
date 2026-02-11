package com.bit.idol.voteservice.dto.event;

import com.bit.idol.voteservice.dto.notification.TargetType;
import com.bit.idol.voteservice.entity.Vote;

public record VoteEvent(Vote vote, String type, TargetType targetType, String targetId) {
}
