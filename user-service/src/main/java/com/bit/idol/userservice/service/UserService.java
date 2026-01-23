package com.bit.idol.userservice.service;

import com.bit.idol.userservice.dto.user.PasswordChangeDto;
import com.bit.idol.userservice.dto.user.UserDto;
import com.bit.idol.userservice.dto.user.UserInfoResponse;
import com.bit.idol.userservice.dto.user.UserUpdateDto;
import com.bit.idol.userservice.entity.Role;
import com.bit.idol.userservice.entity.User;
import com.bit.idol.userservice.entity.UserStatus;
import com.bit.idol.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@lombok.extern.slf4j.Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;
    private final S3Service s3Service;
    private final StringRedisTemplate redisTemplate;

    @Cacheable(value = "user:info:username", key = "#username", unless = "#result == null")
    public UserDto getUserByUsername(String username) {
        log.info("사용자 조회 (Username) - DB 접근: username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return UserDto.fromEntity(user);
    }

    @Cacheable(value = "user:info:id", key = "#userId", unless = "#result == null")
    public UserDto getUserById(int userId) {
        log.info("사용자 조회 (ID) - DB 접근: userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return UserDto.fromEntity(user);
    }

    public List<UserDto> getAllUsers() {
        log.info("전체 사용자 조회 요청");
        return userRepository.findAll().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void registerUser(UserDto userDto) {
        // 1. 이메일 인증 토큰 검증 (소셜 로그인은 제외)
        if (userDto.getProvider() == null) {
            if (userDto.getVerificationToken() == null) {
                throw new RuntimeException("이메일 인증이 필요합니다.");
            }

            String verifiedEmail = redisTemplate.opsForValue().get("verify:token:" + userDto.getVerificationToken());
            if (verifiedEmail == null) {
                throw new RuntimeException("유효하지 않거나 만료된 인증 토큰입니다.");
            }

            if (!verifiedEmail.equals(userDto.getEmail())) {
                throw new RuntimeException("인증된 이메일과 입력한 이메일이 일치하지 않습니다.");
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
        log.info("회원가입 완료: username={}, userId={}", user.getUsername(), user.getId());
    }

    @Transactional
    public UserDto registerSocialUser(UserDto userDto) {
        return userRepository.findByProviderAndProviderId(userDto.getProvider(), userDto.getProviderId())
                .map(UserDto::fromEntity)
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
                    log.info("소셜 회원가입 완료: provider={}, userId={}", userDto.getProvider(), savedUser.getId());
                    return UserDto.fromEntity(savedUser);
                });
    }

    public UserInfoResponse getUserInfo(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserInfoResponse.fromEntity(user);
    }

    @Transactional
    public void updateUserInfo(int userId, UserUpdateDto userUpdateDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userUpdateDto.getNickname() != null)
            user.setNickname(userUpdateDto.getNickname());
        if (userUpdateDto.getEmail() != null)
            user.setEmail(userUpdateDto.getEmail());
        if (userUpdateDto.getPhone() != null)
            user.setPhone(userUpdateDto.getPhone());
        if (userUpdateDto.getAddress() != null)
            user.setAddress(userUpdateDto.getAddress());
        if (userUpdateDto.getImgUrl() != null)
            user.setImgUrl(userUpdateDto.getImgUrl());

        evictUserCache(user);
        log.info("사용자 정보 업데이트 완료: userId={}", userId);
    }

    @Transactional
    public String updateProfileImage(int userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getImgUrl() != null && !user.getImgUrl().isEmpty()) {
            s3Service.deleteFile(user.getImgUrl());
        }

        String fileUrl = s3Service.uploadFile(file);
        user.setImgUrl(fileUrl);
        evictUserCache(user);

        log.info("프로필 이미지 변경 완료: userId={}, url={}", userId, fileUrl);
        return fileUrl;
    }

    @Transactional
    public void changePassword(int userId, PasswordChangeDto passwordChangeDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(passwordChangeDto.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid current password");
        }

        user.setPassword(passwordEncoder.encode(passwordChangeDto.getNewPassword()));
        evictUserCache(user);
        log.info("비밀번호 변경 완료: userId={}", userId);
    }

    // 비밀번호 재설정 (이메일 인증 후) - 반환값 int로 변경
    @Transactional
    public int resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        user.setPassword(passwordEncoder.encode(newPassword));
        evictUserCache(user);
        log.info("비밀번호 재설정 완료: email={}, userId={}", email, user.getId());
        
        return user.getId(); // userId 반환
    }

    @Transactional
    public void withdrawUser(int userId, String checkPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(checkPassword, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        userRepository.delete(user);
        evictUserCache(user);
        log.info("회원 탈퇴 처리 완료: userId={}", userId);
    }

    @Transactional
    public void increaseReportCount(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setReportCount(user.getReportCount() + 1);
        
        if (user.getReportCount() >= 10) {
            user.setStatus(UserStatus.SUSPENDED);
            log.warn("유저 일시 정지 처리됨 (신고 누적): userId={}", userId);
        }

        evictUserCache(user);
        log.info("신고 횟수 증가: userId={}, count={}", userId, user.getReportCount());
    }

    private void evictUserCache(User user) {
        try {
            Objects.requireNonNull(cacheManager.getCache("user:info:id")).evict(user.getId());
            Objects.requireNonNull(cacheManager.getCache("user:info:username")).evict(user.getUsername());
            log.info("캐시 삭제 완료: userId={}, username={}", user.getId(), user.getUsername());
        } catch (Exception e) {
            log.warn("캐시 삭제 중 오류 발생 (무시하고 진행): {}", e.getMessage());
        }
    }
}
