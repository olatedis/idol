package com.bit.idol.userservice.repository;

import com.bit.idol.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    
    // 닉네임 중복 검사 (추가됨)
    boolean existsByNickname(String nickname);
}
