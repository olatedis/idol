package com.bit.idol.userservice.config;

import com.bit.idol.userservice.entity.User;
import com.bit.idol.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        encryptPlainPasswords();
    }

    private void encryptPlainPasswords() {
        int count = 0;
        for (User user : userRepository.findAll()) {
            if (user.getPassword() != null && !isEncrypted(user.getPassword())) {
                log.info("비밀번호 암호화: userId={}, username={}", user.getId(), user.getUsername());
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                userRepository.save(user);
                count++;
            }
        }
        if (count > 0) {
            log.info("총 {}개 계정의 비밀번호 암호화 완료", count);
        }
    }

    private boolean isEncrypted(String password) {
        return password != null && password.startsWith("$2a$") || password.startsWith("$2b$");
    }
}
