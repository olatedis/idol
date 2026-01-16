package com.bit.idol.userservice.service;

import com.bit.idol.userservice.dto.report.ReportRequestDto;
import com.bit.idol.userservice.dto.report.UserStatusChangeDto;
import com.bit.idol.userservice.entity.BanHistory;
import com.bit.idol.userservice.entity.Report;
import com.bit.idol.userservice.entity.User;
import com.bit.idol.userservice.entity.UserStatus;
import com.bit.idol.userservice.repository.BanHistoryRepository;
import com.bit.idol.userservice.repository.ReportRepository;
import com.bit.idol.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final BanHistoryRepository banHistoryRepository;

    // 신고 접수
    @Transactional
    public void reportUser(int reporterId, ReportRequestDto dto) {
        // 1. 신고 대상 조회
        User targetUser = userRepository.findById(dto.getTargetUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. 본인 신고 방지
        if (targetUser.getId() == reporterId) {
            throw new RuntimeException("Cannot report yourself");
        }

        // 3. 신고 내역 저장
        Report report = Report.builder()
                .reporterId(reporterId)
                .targetUserId(dto.getTargetUserId())
                .reason(dto.getReason())
                .description(dto.getDescription())
                .build();
        reportRepository.save(report);

        // 4. 신고 횟수 증가
        targetUser.setReportCount(targetUser.getReportCount() + 1);

        // 5. 자동 제재 로직 (10회 이상 시 일시정지)
        // 이미 정지나 밴 상태가 아닐 때만 적용
        if (targetUser.getReportCount() >= 10 && targetUser.getStatus() == UserStatus.ACTIVE) {
            targetUser.setStatus(UserStatus.SUSPENDED);
            
            // 제재 이력 저장
            saveBanHistory(targetUser.getId(), UserStatus.SUSPENDED, "신고 누적(10회)에 의한 자동 일시정지");
            log.info("유저 자동 일시정지 처리: userId={}", targetUser.getId());
        }
    }

    // 관리자용 상태 변경 (밴/해제)
    @Transactional
    public void changeUserStatus(UserStatusChangeDto dto) {
        User targetUser = userRepository.findById(dto.getTargetUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserStatus oldStatus = targetUser.getStatus();
        UserStatus newStatus = dto.getNewStatus();

        if (oldStatus == newStatus) {
            return; // 변경 사항 없음
        }

        // 상태 변경
        targetUser.setStatus(newStatus);

        // 차단 해제(ACTIVE) 시 신고 횟수 초기화 (선택 사항이지만 보통 초기화함)
        if (newStatus == UserStatus.ACTIVE) {
            targetUser.setReportCount(0);
        }

        // 이력 저장
        saveBanHistory(targetUser.getId(), newStatus, dto.getReason());
        log.info("관리자에 의한 상태 변경: userId={}, {} -> {}", targetUser.getId(), oldStatus, newStatus);
    }

    private void saveBanHistory(int userId, UserStatus status, String reason) {
        BanHistory history = BanHistory.builder()
                .userId(userId)
                .status(status)
                .reason(reason)
                .build();
        banHistoryRepository.save(history);
    }
}
