package com.bit.idol.notifyservice.service;

import com.bit.idol.notifyservice.dto.IdolMessageStackItemResponse;
import com.bit.idol.notifyservice.dto.IdolMessageStackListResponse;
import com.bit.idol.notifyservice.entity.IdolMessageStack;
import com.bit.idol.notifyservice.repository.IdolMessageStackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class IdolMessageStackService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final IdolMessageStackRepository repo;

    public IdolMessageStackService(IdolMessageStackRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public IdolMessageStackListResponse list(int userId) {
        List<IdolMessageStack> stacks = repo.findAllByReceiverIdOrderByLastOccurredAtDesc(userId);

        IdolMessageStackListResponse res = new IdolMessageStackListResponse();
        res.items = new ArrayList<>();

        for (IdolMessageStack s : stacks) {
            IdolMessageStackItemResponse item = new IdolMessageStackItemResponse();
            item.setIdolId(s.getIdolId());
            item.setUnreadCount(s.getUnreadCount());
            item.setLastOccurredAt(s.getLastOccurredAt() == null ? null : s.getLastOccurredAt().format(ISO));
            res.items.add(item);
        }

        return res;
    }

    // 특정 idolId의 unreadCount만 0으로 초기화
    // lastOccurredAt은 "최근 메시지 온 아이돌 위로" 정렬을 위해 유지
    @Transactional
    public void markRead(int userId, long idolId) {
        IdolMessageStack s = repo.findByReceiverIdAndIdolId(userId, idolId).orElse(null);
        if (s == null) return;

        s.setUnreadCount(0);
        repo.save(s);
    }
}
