package com.bit.idol.notifyservice.service;

import com.bit.idol.notifyservice.dto.PreferenceResponse;
import com.bit.idol.notifyservice.dto.UpdatePreferenceRequest;
import com.bit.idol.notifyservice.entity.NotificationPreference;
import com.bit.idol.notifyservice.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferenceService {

    private final NotificationPreferenceRepository repo;

    public PreferenceService(NotificationPreferenceRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public PreferenceResponse getOrCreate(int userId) {
        NotificationPreference pref = repo.findById(userId).orElseGet(() -> repo.save(NotificationPreference.create(userId)));
        return toResponse(pref);
    }

    @Transactional
    public PreferenceResponse update(int userId, UpdatePreferenceRequest req) {
        NotificationPreference pref = repo.findById(userId).orElseGet(() -> NotificationPreference.create(userId));

        pref.setAllEnabled(req.allEnabled);
        pref.setChatEnabled(req.chatEnabled);
        pref.setVoteEnabled(req.voteEnabled);
        pref.setTicketEnabled(req.ticketEnabled);
        pref.setBoardEnabled(req.boardEnabled);

        repo.save(pref);
        return toResponse(pref);
    }

    private PreferenceResponse toResponse(NotificationPreference p) {
        PreferenceResponse res = new PreferenceResponse();

        res.userId = p.getUserId();
        res.allEnabled = p.isAllEnabled();
        res.chatEnabled = p.isChatEnabled();
        res.voteEnabled = p.isVoteEnabled();
        res.ticketEnabled = p.isTicketEnabled();
        res.boardEnabled = p.isBoardEnabled();

        return res;
    }

    @Transactional(readOnly = true)
    public boolean isEnabledForType(int userId, String type) {

        NotificationPreference pref =
                repo.findById(userId)
                        .orElseGet(() -> repo.save(NotificationPreference.create(userId)));

        if (!pref.isAllEnabled()) {
            return false;
        }

        if ("IDOL_MESSAGE".equals(type) || "REPLY_MESSAGE".equals(type) || "CHAT_IDOL_ONLINE".equals(type)) {
            return pref.isChatEnabled();
        }

        if ("BOARD_NEW_POST".equals(type)
                || "BOARD_ADMIN_NOTICE".equals(type)) {
            return pref.isBoardEnabled();
        }

        return true;
    }
}
