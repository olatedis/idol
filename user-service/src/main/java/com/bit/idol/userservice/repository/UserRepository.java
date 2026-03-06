package com.bit.idol.userservice.repository;

import com.bit.idol.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    // 닉네임 중복 검사 (추가됨)
    boolean existsByNickname(String nickname);

    // 요주의 인물 조회 (ACTIVE 이면서 신고가 누적된 유저)
    List<User> findByStatusAndReportCountGreaterThanOrderByReportCountDesc(
            com.bit.idol.userservice.entity.UserStatus status, int reportCount);

    // 관리자 유저 검색
    List<User> findByNicknameContainingIgnoreCaseOrEmailContainingIgnoreCase(String nickname, String email);
}
