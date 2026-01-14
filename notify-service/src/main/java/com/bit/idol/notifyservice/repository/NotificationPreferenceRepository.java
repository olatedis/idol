package com.bit.idol.notifyservice.repository;

import com.bit.idol.notifyservice.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Integer> {
    void deleteByUserId(int userId);
}
