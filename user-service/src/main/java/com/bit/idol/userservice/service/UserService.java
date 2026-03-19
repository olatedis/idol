package com.bit.idol.userservice.service;

import com.bit.idol.userservice.client.SubscriptionFeignClient;
import com.bit.idol.userservice.document.UserView;
import com.bit.idol.userservice.dto.UserMyPageDto;
import com.bit.idol.userservice.dto.event.UserEvent;
import com.bit.idol.userservice.dto.notification.NotificationEventDto;
import com.bit.idol.userservice.dto.notification.TargetType;
import com.bit.idol.userservice.dto.user.PasswordChangeDto;
import com.bit.idol.userservice.dto.user.UserDto;
import com.bit.idol.userservice.dto.user.UserUpdateDto;
import com.bit.idol.userservice.dto.user.BanHistoryDto;
import com.bit.idol.userservice.entity.BanHistory;
import com.bit.idol.userservice.entity.Role;
import com.bit.idol.userservice.entity.User;
import com.bit.idol.userservice.entity.UserStatus;
import com.bit.idol.userservice.producer.NotificationProducer;
import com.bit.idol.userservice.repository.BanHistoryRepository;
import com.bit.idol.userservice.repository.UserRepository;
import com.bit.idol.userservice.repository.UserViewRepository;
import com.bit.idol.userservice.repository.AgencyAccountRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@lombok.extern.slf4j.Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserViewRepository userViewRepository;
    private final BanHistoryRepository banHistoryRepository;
    private final AgencyAccountRepository agencyAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;
    private final S3Service s3Service;
    private final StringRedisTemplate redisTemplate;
    private final NotificationProducer notificationProducer;
    private final SubscriptionFeignClient subscriptionFeignClient;
    private final ApplicationEventPublisher eventPublisher;

    // 닉네임 중복 검사
    public boolean checkNicknameAvailability(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    // 마이페이지 정보 조회 (Aggregation)
    @CircuitBreaker(name = "subscription-service", fallbackMethod = "getMyPageInfoFallback")
    public UserMyPageDto getMyPageInfo(int userId) {
        UserDto user = getUserById(userId);

        int subscriptionCount = subscriptionFeignClient.getMySubscriptionCount(userId);

        Integer agencyId = null;
        if (user.getRole() == Role.AGENCY) {
            agencyId = agencyAccountRepository.findByUser_Id(userId)
                    .map(account -> account.getAgency().getId())
                    .orElse(null);
        }

        return UserMyPageDto.builder()
                .id(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .profileImage(user.getImgUrl())
                .role(user.getRole())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                .status(user.getStatus())
                .subscriptionCount(subscriptionCount)
                .agencyId(agencyId)
                .build();
    }

    public UserMyPageDto getMyPageInfoFallback(int userId, Throwable t) {
        log.error("subscription-service 통신 장애 (마이페이지 구독 갯수 조회): {}", t.getMessage());
        UserDto user = getUserById(userId);

        Integer agencyId = null;
        if (user.getRole() == Role.AGENCY) {
            agencyId = agencyAccountRepository.findByUser_Id(userId)
                    .map(account -> account.getAgency().getId())
                    .orElse(null);
        }

        return UserMyPageDto.builder()
                .id(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .profileImage(user.getImgUrl())
                .role(user.getRole())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                .status(user.getStatus())
                .subscriptionCount(0)
                .agencyId(agencyId)
                .build();
    }

    // 일반 조회 (MongoDB 사용 - 비밀번호 없음)
    @Cacheable(value = "user:info:username", key = "#username", unless = "#result == null")
    public UserDto getUserByUsername(String username) {
        return userViewRepository.findByUsername(username)
                .map(this::convertViewToDto)
                .orElseGet(() -> {
                    User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("해당 사용자 이름의 사용자를 찾을 수 없습니다: " + username));
                    return UserDto.fromEntity(user);
                });
    }

    // 로그인용 조회 (MySQL 사용 - 비밀번호 포함)
    public UserDto getUserForLogin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("해당 사용자 이름의 사용자를 찾을 수 없습니다: " + username));
        return UserDto.fromEntity(user);
    }

    @Cacheable(value = "user:info:id", key = "#userId", unless = "#result == null")
    public UserDto getUserById(int userId) {
        return userViewRepository.findById(userId)
                .map(this::convertViewToDto)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("해당 ID의 사용자를 찾을 수 없습니다: " + userId));
                    return UserDto.fromEntity(user);
                });
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    public Map<Integer, UserDto> getUsersByIds(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        return userRepository.findAllById(userIds).stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toMap(UserDto::getUserId, user -> user));
    }

    public Page<UserDto> getAllUsersWithPaging(Pageable pageable, com.bit.idol.userservice.entity.UserStatus status) {
        if (status == null) {
            return userRepository.findAll(pageable).map(UserDto::fromEntity);
        } else {
            return userRepository.findByStatus(status, pageable).map(UserDto::fromEntity);
        }
    }

    @Transactional
    public void registerUser(UserDto userDto) {
        if (userDto.getProvider() == null) {
            if (userDto.getVerificationToken() == null) {
                throw new RuntimeException("이메일 인증이 필요합니다.");
            }
            String verifiedEmail = redisTemplate.opsForValue().get("verify:token:" + userDto.getVerificationToken());
            if (verifiedEmail == null || !verifiedEmail.equals(userDto.getEmail())) {
                throw new RuntimeException("유효하지 않은 인증 토큰입니다.");
            }
            redisTemplate.delete("verify:token:" + userDto.getVerificationToken());
        }

        if (userRepository.findByUsername(userDto.getUsername()).isPresent()) {
            throw new RuntimeException("이미 존재하는 사용자 이름입니다.");
        }

        if (userRepository.existsByNickname(userDto.getNickname())) {
            throw new RuntimeException("이미 존재하는 닉네임입니다.");
        }

        User user = User.builder()
                .username(userDto.getUsername())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .nickname(userDto.getNickname())
                .email(userDto.getEmail())
                .phone(userDto.getPhone())
                .address(userDto.getAddress())
                .role(userDto.getRole() != null ? userDto.getRole() : Role.USER)
                .imgUrl(userDto.getImgUrl())
                .build();

        userRepository.save(user);

        // 이벤트 발행 (커밋 후 실행됨)
        eventPublisher.publishEvent(new UserEvent(user.getId(), "CREATE", user.getStatus().name()));

        log.info("회원가입 완료: username={}, userId={}", user.getUsername(), user.getId());
    }

    @Transactional
    public UserDto registerSocialUser(UserDto userDto) {
        return userRepository.findByProviderAndProviderId(userDto.getProvider(), userDto.getProviderId())
                .map(user -> {
                    return UserDto.fromEntity(user);
                })
                .orElseGet(() -> {
                    String randomPassword = UUID.randomUUID().toString();
                    String socialUsername = userDto.getProvider() + "_" + userDto.getProviderId();

                    // 랜덤 닉네임 생성 (예: 팬돌이_1234)
                    String randomNickname = "팬돌이_" + (int) (Math.random() * 10000);
                    while (userRepository.existsByNickname(randomNickname)) {
                        randomNickname = "팬돌이_" + (int) (Math.random() * 10000);
                    }

                    User newUser = User.builder()
                            .username(socialUsername)
                            .password(passwordEncoder.encode(randomPassword))
                            .nickname(randomNickname) // 랜덤 닉네임
                            .realName(userDto.getRealName()) // 실명 (소셜 이름) 저장
                            .email(userDto.getEmail())
                            .role(Role.USER)
                            .provider(userDto.getProvider())
                            .providerId(userDto.getProviderId())
                            .imgUrl(userDto.getImgUrl())
                            .build();

                    User savedUser = userRepository.save(newUser);

                    // 이벤트 발행
                    eventPublisher.publishEvent(new UserEvent(savedUser.getId(), "CREATE", savedUser.getStatus().name()));

                    log.info("소셜 회원가입 완료: provider={}, userId={}", userDto.getProvider(), savedUser.getId());
                    return UserDto.fromEntity(savedUser);
                });
    }

    @Transactional
    @CachePut(value = "user:info:id", key = "#userId")
    public UserDto updateUserInfo(int userId, UserUpdateDto userUpdateDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (userUpdateDto.getNickname() != null) {
            if (!user.getNickname().equals(userUpdateDto.getNickname()) &&
                    userRepository.existsByNickname(userUpdateDto.getNickname())) {
                throw new RuntimeException("이미 존재하는 닉네임입니다.");
            }
            user.setNickname(userUpdateDto.getNickname());
        }

        if (userUpdateDto.getEmail() != null)
            user.setEmail(userUpdateDto.getEmail());
        if (userUpdateDto.getPhone() != null)
            user.setPhone(userUpdateDto.getPhone());
        if (userUpdateDto.getAddress() != null)
            user.setAddress(userUpdateDto.getAddress());
        if (userUpdateDto.getImgUrl() != null)
            user.setImgUrl(userUpdateDto.getImgUrl());

        // 이벤트 발행
        eventPublisher.publishEvent(new UserEvent(user.getId(), "UPDATE", user.getStatus().name()));
        updateUsernameCache(user);

        log.info("사용자 정보 업데이트 완료: userId={}", userId);
        return UserDto.fromEntity(user);
    }

    @Transactional
    @CachePut(value = "user:info:id", key = "#userId")
    public UserDto updateProfileImage(int userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        String oldImgUrl = user.getImgUrl();
        String fileUrl = null;

        try {
            // 1. S3 업로드
            fileUrl = s3Service.uploadFile(file);

            // 2. DB 업데이트
            user.setImgUrl(fileUrl);
            userRepository.saveAndFlush(user); // 즉시 반영하여 DB 에러 확인

            // 3. 기존 이미지 삭제 (DB 성공 시)
            if (oldImgUrl != null && !oldImgUrl.isEmpty()) {
                try {
                    s3Service.deleteFile(oldImgUrl);
                } catch (Exception e) {
                    log.warn("기존 프로필 이미지 삭제 실패 (S3): {}", oldImgUrl);
                }
            }
        } catch (Exception e) {
            // DB 저장 실패 시 업로드된 새 이미지 삭제 (보상 트랜잭션)
            if (fileUrl != null) {
                try {
                    s3Service.deleteFile(fileUrl);
                } catch (Exception s3Ex) {
                    log.error("롤백 중 S3 파일 삭제 실패: {}", fileUrl);
                }
            }
            throw new RuntimeException("프로필 이미지 업데이트 실패", e);
        }

        // 이벤트 발행
        eventPublisher.publishEvent(new UserEvent(user.getId(), "UPDATE", user.getStatus().name()));
        updateUsernameCache(user);

        return UserDto.fromEntity(user);
    }

    @Transactional
    @CachePut(value = "user:info:id", key = "#userId")
    public UserDto changePassword(int userId, PasswordChangeDto passwordChangeDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(passwordChangeDto.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        user.setPassword(passwordEncoder.encode(passwordChangeDto.getNewPassword()));

        sendPasswordChangedNotification(user);

        return UserDto.fromEntity(user);
    }

    @Transactional
    @CachePut(value = "user:info:id", key = "#result.userId")
    public UserDto resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 이메일의 사용자를 찾을 수 없습니다: " + email));

        user.setPassword(passwordEncoder.encode(newPassword));

        sendPasswordChangedNotification(user);

        return UserDto.fromEntity(user);
    }

    @Transactional
    public void withdrawUser(int userId, String checkPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 소셜 로그인 회원이 아닐 경우에만 비밀번호 일치 검사 수행
        if (user.getProvider() == null || user.getProvider().isEmpty()) {
            if (!passwordEncoder.matches(checkPassword, user.getPassword())) {
                throw new RuntimeException("비밀번호가 일치하지 않습니다.");
            }
        }

        // 1. 프로필 이미지 삭제 추가
        if (user.getImgUrl() != null && !user.getImgUrl().isEmpty()) {
            try {
                s3Service.deleteFile(user.getImgUrl());
            } catch (Exception e) {
                log.warn("회원 탈퇴 시 프로필 이미지 삭제 실패: {}", user.getImgUrl());
            }
        }

        // 2. 논리 삭제 처리
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setStatus(UserStatus.WITHDRAWN); // 상태도 WITHDRAWN으로 변경
        
        // 3. 중복 방지를 위해 식별 정보 무작위 변경 (닉네임 등)
        user.setNickname(user.getNickname() + "_withdrawn_" + UUID.randomUUID().toString().substring(0, 8));
        
        userRepository.save(user);

        // 이벤트 발행
        eventPublisher.publishEvent(new UserEvent(userId, "DELETE", "DELETED"));

        evictUserCache(user);
    }

    @Transactional
    @CachePut(value = "user:info:id", key = "#userId")
    public UserDto increaseReportCount(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        user.setReportCount(user.getReportCount() + 1);

        if (user.getReportCount() >= 5 && user.getStatus() == UserStatus.ACTIVE) {
            user.setStatus(UserStatus.RESTRICTED);

            BanHistory history = BanHistory.builder()
                    .userId(userId)
                    .status(UserStatus.RESTRICTED)
                    .reason("신고 누적(5회)에 의한 자동 활동 제한 (쓰기 금지)")
                    .build();
            banHistoryRepository.save(history);

            log.warn("유저 자동 활동 제한 처리: userId={}", userId);
        }

        // 신고 4회 도달 시 경고 알림
        if (user.getReportCount() == 4) {
            NotificationEventDto event = NotificationEventDto.builder()
                    .eventId(UUID.randomUUID().toString())
                    .type("REPORT_RECEIVED")
                    .targetType(TargetType.USER)
                    .targetId(String.valueOf(userId))
                    .args(java.util.Map.of("reportCount", String.valueOf(user.getReportCount())))
                    .redirectUrl("#")
                    .occurredAt(java.time.LocalDateTime.now())
                    .build();
            notificationProducer.send(event);
        }

        // 이벤트 발행
        eventPublisher.publishEvent(new UserEvent(user.getId(), "UPDATE", user.getStatus().name()));
        updateUsernameCache(user);

        return UserDto.fromEntity(user);
    }

    @Transactional
    @CachePut(value = "user:info:id", key = "#userId")
    public UserDto updateUserStatus(int userId, UserStatus newStatus, String reason, Integer durationDays) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (user.getStatus() == newStatus)
            return UserDto.fromEntity(user);

        user.setStatus(newStatus);

        if (newStatus == UserStatus.ACTIVE) {
            user.setReportCount(0); // 징계 해제 시 카운트 초기화
            user.setSuspendedUntil(null);
        } else if (durationDays != null && durationDays > 0) {
            user.setSuspendedUntil(LocalDateTime.now().plusDays(durationDays));
        } else if (newStatus == UserStatus.BANNED) {
            user.setSuspendedUntil(null); // 영구정지 등
        }

        BanHistory history = BanHistory.builder()
                .userId(userId)
                .status(newStatus)
                .reason(reason)
                .build();
        banHistoryRepository.save(history);
        userRepository.save(user);

        // 이벤트 발행
        eventPublisher.publishEvent(new UserEvent(user.getId(), "UPDATE", user.getStatus().name()));
        updateUsernameCache(user);

        log.info("유저 상태 변경 완료: userId={}, status={}", userId, newStatus);

        // 상태 변경 알림 발행
        NotificationEventDto statusEvent = NotificationEventDto.builder()
                .eventId(UUID.randomUUID().toString())
                .type("ACCOUNT_STATUS_CHANGED")
                .targetType(TargetType.USER)
                .targetId(String.valueOf(userId))
                .args(java.util.Map.of(
                        "status", newStatus.name(),
                        "reason", reason != null ? reason : ""
                ))
                .occurredAt(java.time.LocalDateTime.now())
                .build();
        notificationProducer.send(statusEvent);

        return UserDto.fromEntity(user);
    }

    // 유저 제재 내역 리스트 조회
    public List<BanHistoryDto> getUserBanHistory(int userId) {
        return banHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(BanHistoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    private UserDto convertViewToDto(UserView view) {
        return UserDto.builder()
                .userId(view.getId())
                .username(view.getUsername())
                .nickname(view.getNickname())
                .email(view.getEmail())
                .phone(view.getPhone())
                .address(view.getAddress())
                .imgUrl(view.getImgUrl())
                .role(Role.valueOf(view.getRole()))
                .provider(view.getProvider())
                .providerId(view.getProviderId())
                .status(UserStatus.valueOf(view.getStatus()))
                .reportCount(view.getReportCount())
                .suspendedUntil(view.getSuspendedUntil())
                .createdAt(view.getCreatedAt())
                .build();
    }

    private void evictUserCache(User user) {
        try {
            Objects.requireNonNull(cacheManager.getCache("user:info:id")).evict(user.getId());
            Objects.requireNonNull(cacheManager.getCache("user:info:username")).evict(user.getUsername());
        } catch (Exception e) {
            log.warn("캐시 삭제 중 오류 발생: {}", e.getMessage());
        }
    }

    private void updateUsernameCache(User user) {
        try {
            Objects.requireNonNull(cacheManager.getCache("user:info:username")).put(user.getUsername(),
                    UserDto.fromEntity(user));
        } catch (Exception e) {
            log.warn("username 캐시 갱신 실패: {}", e.getMessage());
        }
    }

    private void sendPasswordChangedNotification(User user) {
        NotificationEventDto event = NotificationEventDto.builder()
                .eventId(UUID.randomUUID().toString())
                .type("PASSWORD_CHANGED")
                .targetType(TargetType.USER)
                .targetId(String.valueOf(user.getId()))
                .occurredAt(LocalDateTime.now())
                .build();

        notificationProducer.send(event);
    }
}
