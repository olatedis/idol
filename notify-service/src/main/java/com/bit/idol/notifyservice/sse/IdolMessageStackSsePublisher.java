package com.bit.idol.notifyservice.sse;

import com.bit.idol.notifyservice.dto.IdolMessageStackSsePayload;
import com.bit.idol.notifyservice.entity.IdolMessageStack;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class IdolMessageStackSsePublisher {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final SseEmitterRegistry registry;

    public IdolMessageStackSsePublisher(SseEmitterRegistry registry) {
        this.registry = registry;
    }

    public void pushToUser(int userId, IdolMessageStack stack) {
        if (stack == null) return;

        IdolMessageStackSsePayload p = new IdolMessageStackSsePayload();
        p.setIdolId(stack.getIdolId());
        p.setUnreadCount(stack.getUnreadCount());
        p.setLastOccurredAt(stack.getLastOccurredAt() != null ? stack.getLastOccurredAt().format(ISO) : null);

        // 프론트는 이 이벤트만 받아서 "idolId별 뱃지 숫자" 갱신하면 됨
        registry.send(userId, "idol_message_stack", p);
    }
}
