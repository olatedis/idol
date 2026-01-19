package com.bit.idol.userservice.repository;

import com.bit.idol.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    
    // 이메일로 유저 찾기 (비밀번호 재설정용)
    Optional<User> findByEmail(String email);
    
    // 소셜 로그인용 조회 메서드
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}
