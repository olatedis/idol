package com.bit.idol.userservice.service;

import com.bit.idol.userservice.document.UserView;
import com.bit.idol.userservice.dto.user.PasswordChangeDto;
import com.bit.idol.userservice.dto.user.UserDto;
import com.bit.idol.userservice.dto.user.UserInfoResponse;
import com.bit.idol.userservice.dto.user.UserUpdateDto;
import com.bit.idol.userservice.entity.BanHistory;
import com.bit.idol.userservice.entity.Role;
import com.bit.idol.userservice.entity.User;
import com.bit.idol.userservice.entity.UserStatus;
import com.bit.idol.userservice.repository.BanHistoryRepository;
import com.bit.idol.userservice.repository.UserRepository;
import com.bit.idol.userservice.repository.UserViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@lombok.extern.slf4j.Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserViewRepository userViewRepository;
    private final BanHistoryRepository banHistoryRepository; // 추가됨
    private final BCryptPasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;
    private final S3Service s3Service;
    private final StringRedisTemplate redisTemplate;

    // ... (조회 메서드 생략 - 기존과 동일) ...

    @Cacheable(value = "user:info:username", key = "#username", unless = "#result == null")
    public UserDto getUserByUsername(String username) {
        return userViewRepository.findByUsername(username)
                .map(this::convertViewToDto)
                .orElseGet(() -> {
                    User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
                    syncToMongo(user);
                    return UserDto.fromEntity(user);
                });
    }

    @Cacheable(value = "user:info:id", key = "#userId", unless = "#result == null")
    public UserDto getUserById(int userId) {
        return userViewRepository.findById(userId)
                .map(this::convertViewToDto)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
                    syncToMongo(user);
                    return UserDto.fromEntity(user);
                });
    }

    public List<UserDto> getAllUsers() {
        return userViewRepository.findAll().stream()
                .map(this::convertViewToDto)
                .collect(Collectors.toList());
    }

    // ... (회원가입, 수정 메서드 생략 - 기존과 동일) ...

    @Transactional
    public void registerUser(UserDto userDto) {
        // (기존 코드 유지)
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
            throw new RuntimeException("Username already exists");
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
        syncToMongo(user);
        log.info("회원가입 완료: username={}, userId={}", user.getUsername(), user.getId());
    }

    @Transactional
    public UserDto registerSocialUser(UserDto userDto) {
        // (기존 코드 유지)
        return userRepository.findByProviderAndProviderId(userDto.getProvider(), userDto.getProviderId())
                .map(user -> {
                    syncToMongo(user);
                    return UserDto.fromEntity(user);
                })
                .orElseGet(() -> {
                    String randomPassword = UUID.randomUUID().toString();
                    String socialUsername = userDto.getProvider() + "_" + userDto.getProviderId();

                    User newUser = User.builder()
                            .username(socialUsername)
                            .password(passwordEncoder.encode(randomPassword))
                            .nickname(userDto.getNickname())
                            .email(userDto.getEmail())
                            .role(Role.USER)
                            .provider(userDto.getProvider())
                            .providerId(userDto.getProviderId())
                            .imgUrl(userDto.getImgUrl())
                            .build();

                    User savedUser = userRepository.save(newUser);
                    syncToMongo(savedUser);
                    log.info("소셜 회원가입 완료: provider={}, userId={}", userDto.getProvider(), savedUser.getId());
                    return UserDto.fromEntity(savedUser);
                });
    }

    @Transactional
    public void updateUserInfo(int userId, UserUpdateDto userUpdateDto) {
        // (기존 코드 유지)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userUpdateDto.getNickname() != null) user.setNickname(userUpdateDto.getNickname());
        if (userUpdateDto.getEmail() != null) user.setEmail(userUpdateDto.getEmail());
        if (userUpdateDto.getPhone() != null) user.setPhone(userUpdateDto.getPhone());
        if (userUpdateDto.getAddress() != null) user.setAddress(userUpdateDto.getAddress());
        if (userUpdateDto.getImgUrl() != null) user.setImgUrl(userUpdateDto.getImgUrl());

        syncToMongo(user);
        evictUserCache(user);
        log.info("사용자 정보 업데이트 완료: userId={}", userId);
    }

    @Transactional
    public String updateProfileImage(int userId, MultipartFile file) {
        // (기존 코드 유지)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getImgUrl() != null && !user.getImgUrl().isEmpty()) {
            s3Service.deleteFile(user.getImgUrl());
        }

        String fileUrl = s3Service.uploadFile(file);
        user.setImgUrl(fileUrl);
        
        syncToMongo(user);
        evictUserCache(user);
        return fileUrl;
    }

    @Transactional
    public void changePassword(int userId, PasswordChangeDto passwordChangeDto) {
        // (기존 코드 유지)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(passwordChangeDto.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid current password");
        }

        user.setPassword(passwordEncoder.encode(passwordChangeDto.getNewPassword()));
        evictUserCache(user);
    }

    @Transactional
    public int resetPassword(String email, String newPassword) {
        // (기존 코드 유지)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        user.setPassword(passwordEncoder.encode(newPassword));
        evictUserCache(user);
        return user.getId();
    }

    @Transactional
    public void withdrawUser(int userId, String checkPassword) {
        // (기존 코드 유지)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(checkPassword, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        userRepository.delete(user);
        userViewRepository.deleteById(userId);
        evictUserCache(user);
    }

    // --- 신고 및 제재 관련 (수정됨) ---

    @Transactional
    public void increaseReportCount(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setReportCount(user.getReportCount() + 1);
        
        // 10회 이상이고 정지 상태가 아니면 자동 정지
        if (user.getReportCount() >= 10 && user.getStatus() == UserStatus.ACTIVE) {
            user.setStatus(UserStatus.SUSPENDED);
            
            // 제재 이력 저장
            BanHistory history = BanHistory.builder()
                    .userId(userId)
                    .status(UserStatus.SUSPENDED)
                    .reason("신고 누적(10회)에 의한 자동 일시정지")
                    .build();
            banHistoryRepository.save(history);
            
            log.warn("유저 자동 일시정지 처리: userId={}", userId);
        }
        
        syncToMongo(user);
        evictUserCache(user);
    }

    // 관리자용 상태 변경 (추가됨)
    @Transactional
    public void updateUserStatus(int userId, UserStatus newStatus, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() == newStatus) return;

        user.setStatus(newStatus);

        // 정지 해제 시 신고 횟수 초기화
        if (newStatus == UserStatus.ACTIVE) {
            user.setReportCount(0);
        }

        // 제재 이력 저장
        BanHistory history = BanHistory.builder()
                .userId(userId)
                .status(newStatus)
                .reason(reason)
                .build();
        banHistoryRepository.save(history);

        syncToMongo(user);
        evictUserCache(user);
        
        log.info("유저 상태 변경 완료: userId={}, status={}", userId, newStatus);
    }

    // --- Helper Methods ---

    private void syncToMongo(User user) {
        try {
            UserView userView = UserView.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .address(user.getAddress())
                    .imgUrl(user.getImgUrl())
                    .role(user.getRole().name())
                    .provider(user.getProvider())
                    .providerId(user.getProviderId())
                    .status(user.getStatus().name())
                    .reportCount(user.getReportCount())
                    .build();
            
            userViewRepository.save(userView);
        } catch (Exception e) {
            log.error("MongoDB 동기화 실패: {}", e.getMessage());
        }
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
}
