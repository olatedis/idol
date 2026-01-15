package com.bit.idol.userservice.service;

import com.bit.idol.userservice.dto.PasswordChangeDto;
import com.bit.idol.userservice.dto.UserDto;
import com.bit.idol.userservice.dto.UserInfoResponse;
import com.bit.idol.userservice.dto.UserUpdateDto;
import com.bit.idol.userservice.entity.Role;
import com.bit.idol.userservice.entity.User;
import com.bit.idol.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@lombok.extern.slf4j.Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // 캐싱 적용: username으로 조회 시 Redis 캐시 사용
    @Cacheable(value = "user:info:username", key = "#username", unless = "#result == null")
    public UserDto getUserByUsername(String username) {
        log.info("사용자 조회 (Username) - DB 접근: username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return UserDto.fromEntity(user);
    }

    // 캐싱 적용: userId로 조회 시 Redis 캐시 사용
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
        // 중복 체크
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

    public UserInfoResponse getUserInfo(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserInfoResponse.fromEntity(user);
    }

    // 정보 수정 시 캐시 삭제 (정합성 유지)
    @Transactional
    @CacheEvict(value = {"user:info:username", "user:info:id"}, allEntries = true) 
    // 주의: allEntries=true는 모든 유저 캐시를 날리므로 비효율적일 수 있음.
    // 더 정교하게 하려면 username과 id를 각각 지정해서 지워야 함.
    // 하지만 updateUserInfo에는 username 파라미터가 없어서 일단 전체 삭제로 처리하거나,
    // User 객체를 조회한 뒤 그 username으로 개별 삭제해야 함.
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

        log.info("사용자 정보 업데이트 완료: userId={}", userId);
    }

    @Transactional
    @CacheEvict(value = {"user:info:username", "user:info:id"}, allEntries = true)
    public void changePassword(int userId, PasswordChangeDto passwordChangeDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(passwordChangeDto.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid current password");
        }

        user.setPassword(passwordEncoder.encode(passwordChangeDto.getNewPassword()));
        log.info("비밀번호 변경 완료: userId={}", userId);
    }

    @Transactional
    @CacheEvict(value = {"user:info:username", "user:info:id"}, allEntries = true)
    public void withdrawUser(int userId, String checkPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(checkPassword, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        userRepository.delete(user);
        log.info("회원 탈퇴 처리 완료: userId={}", userId);
    }
}
