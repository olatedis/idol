package com.bit.idol.notifyservice.service;

import com.bit.idol.notifyservice.dto.IdolMessageStackItemResponse;
import com.bit.idol.notifyservice.dto.IdolMessageStackListResponse;
import com.bit.idol.notifyservice.entity.IdolMessageStack;
import com.bit.idol.notifyservice.repository.IdolMessageStackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    /**
     * IDOL_MESSAGE 도착 시:
     * - (receiverId, idolId) 스택 +1
     * - lastOccurredAt 최신값 유지
     */
    @Transactional
    public IdolMessageStack increase(int receiverId, long idolId, LocalDateTime occurredAt) {
        repo.upsertIncrease(receiverId, idolId, occurredAt);
        // upsert 후 최신 row 조회(알림 SSE payload에 필요)
        return repo.findByReceiverIdAndIdolId(receiverId, idolId).orElse(null);
    }

    /**
     * 채팅방 들어가면:
     * - 해당 idolId 스택 unreadCount=0
     * - lastOccurredAt은 유지
     */
    @Transactional
    public void reset(int receiverId, long idolId) {
        repo.resetUnread(receiverId, idolId);
    }

    @Transactional
    public void resetAll(int receiverId) {
        repo.resetAllUnread(receiverId);
    }

    @Transactional
    public int deleteOne(int receiverId, long idolId) {
        return repo.deleteOneByReceiverIdAndIdolId(receiverId, idolId);
    }

    @Transactional
    public int deleteMany(int receiverId, List<Long> idolIds) {
        if (idolIds == null || idolIds.isEmpty()) {
            return 0;
        }
        return repo.deleteManyByReceiverIdAndIdolIds(receiverId, idolIds);
    }

    /**
     * 아이돌별 unread 목록(최근 메시지 온 아이돌이 위로)
     */
    @Transactional(readOnly = true)
    public IdolMessageStackListResponse list(int receiverId) {
        List<IdolMessageStack> list = repo.findAllByReceiverIdOrderByLastOccurredAtDesc(receiverId);

        IdolMessageStackListResponse res = new IdolMessageStackListResponse();
        res.items = new ArrayList<>();

        for (IdolMessageStack s : list) {
            IdolMessageStackItemResponse item = new IdolMessageStackItemResponse();
            item.setIdolId(s.getIdolId());
            item.setUnreadCount(s.getUnreadCount());
            item.setLastOccurredAt(s.getLastOccurredAt() != null ? s.getLastOccurredAt().format(ISO) : null);
            res.items.add(item);
        }

        return res;
    }
}
