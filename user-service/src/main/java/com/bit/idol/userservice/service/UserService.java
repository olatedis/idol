package com.bit.idol.userservice.service;

import com.bit.idol.userservice.dto.PasswordChangeDto;
import com.bit.idol.userservice.dto.UserDto;
import com.bit.idol.userservice.dto.UserInfoResponse;
import com.bit.idol.userservice.dto.UserUpdateDto;
import com.bit.idol.userservice.entity.Role;
import com.bit.idol.userservice.entity.User;
import com.bit.idol.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return UserDto.fromEntity(user);
    }

    public UserDto getUserById(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public void registerUser(UserDto userDto) {
        // 중복 체크
        if (userRepository.findByUsername(userDto.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = User.builder()
                .username(userDto.getUsername())
                .password(passwordEncoder.encode(userDto.getPassword())) // 비밀번호 암호화
                .nickname(userDto.getNickname())
                .email(userDto.getEmail())
                .phone(userDto.getPhone())
                .address(userDto.getAddress())
                .role(userDto.getRole() != null ? userDto.getRole() : Role.USER) // 기본 권한 설정
                .imgUrl(userDto.getImgUrl())
                .build();

        userRepository.save(user);
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
    }

    @Transactional
    public void changePassword(int userId, PasswordChangeDto passwordChangeDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(passwordChangeDto.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid current password");
        }

        user.setPassword(passwordEncoder.encode(passwordChangeDto.getNewPassword()));
    }

    @Transactional
    public void withdrawUser(int userId, String checkPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(checkPassword, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        userRepository.delete(user);
    }
}
