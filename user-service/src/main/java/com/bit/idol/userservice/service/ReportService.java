package com.bit.idol.userservice.service;

import com.bit.idol.userservice.dto.report.ReportRequestDto;
import com.bit.idol.userservice.dto.report.UserStatusChangeDto;
import com.bit.idol.userservice.entity.Report;
import com.bit.idol.userservice.entity.Role;
import com.bit.idol.userservice.entity.User;
import com.bit.idol.userservice.entity.UserStatus;
import com.bit.idol.userservice.repository.ReportRepository;
import com.bit.idol.userservice.repository.UserRepository;
import com.bit.idol.userservice.dto.report.ReportDto;
import com.bit.idol.userservice.dto.user.UserDto;

import java.util.List;
import java.util.stream.Collectors;
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
    private final UserService userService; // UserService 위임

    // 신고 접수
    @Transactional
    public void reportUser(int reporterId, ReportRequestDto dto) {
        // 1. 신고 대상 존재 확인
        User targetUser = userRepository.findById(dto.getTargetUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. 본인 신고 방지
        if (targetUser.getId() == reporterId) {
            throw new RuntimeException("Cannot report yourself");
        }

        // 아이돌은 신고 대상이 될 수 없음
        if (targetUser.getRole() == Role.IDOL) {
            throw new RuntimeException("아이돌은 신고 대상이 될 수 없습니다.");
        }

        // 3. 중복 신고 방지
        if (reportRepository.existsByReporterIdAndTargetUserId(reporterId, dto.getTargetUserId())) {
            throw new RuntimeException("You have already reported this user.");
        }

        // 4. 신고 내역 저장
        Report report = Report.builder()
                .reporterId(reporterId)
                .targetUserId(dto.getTargetUserId())
                .reason(dto.getReason())
                .description(dto.getDescription())
                .build();
        reportRepository.save(report);

        // 5. 신고 횟수 증가 및 자동 제재 처리 (UserService 위임)
        userService.increaseReportCount(dto.getTargetUserId());

        log.info("신고 접수 완료: reporter={}, target={}", reporterId, dto.getTargetUserId());
    }

    // 관리자용 상태 변경 (밴/해제)
    @Transactional
    public void changeUserStatus(UserStatusChangeDto dto) {
        // UserService에 위임 (상태 변경, 이력 저장, MongoDB 동기화 모두 처리됨)
        userService.updateUserStatus(dto.getTargetUserId(), dto.getNewStatus(), dto.getReason(), dto.getDurationDays());

        log.info("관리자 상태 변경 요청 처리 완료: target={}", dto.getTargetUserId());
    }

    // 요주의 인물 조회 (관리자 대기열)
    @Transactional(readOnly = true)
    public List<UserDto> getActiveUsersWithReports() {
        return userRepository.findByStatusAndReportCountGreaterThanOrderByReportCountDesc(UserStatus.ACTIVE, 0)
                .stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 관리자 유저 검색
    @Transactional(readOnly = true)
    public List<UserDto> searchUsersForAdmin(String keyword) {
        return userRepository.findByNicknameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 특정 유저의 피신고 내역 리스트 조회
    @Transactional(readOnly = true)
    public List<ReportDto> getUserReportHistory(int targetUserId) {
        return reportRepository.findByTargetUserId(targetUserId)
                .stream()
                .map(ReportDto::fromEntity)
                .collect(Collectors.toList());
    }
}
