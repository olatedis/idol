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

        pref.setChatEnabled(req.chatEnabled);
        pref.setVoteEnabled(req.voteEnabled);
        pref.setTicketEnabled(req.ticketEnabled);
        pref.setNoticeEnabled(req.noticeEnabled);

        repo.save(pref);
        return toResponse(pref);
    }

    private PreferenceResponse toResponse(NotificationPreference p) {
        PreferenceResponse res = new PreferenceResponse();
        res.userId = p.getUserId();
        res.chatEnabled = p.isChatEnabled();
        res.voteEnabled = p.isVoteEnabled();
        res.ticketEnabled = p.isTicketEnabled();
        res.noticeEnabled = p.isNoticeEnabled();
        return res;
    }
}
