package com.bit.idol.userservice.consumer;

import com.bit.idol.userservice.document.UserView;
import com.bit.idol.userservice.dto.kafka.UserEventDto;
import com.bit.idol.userservice.entity.User;
import com.bit.idol.userservice.repository.UserRepository;
import com.bit.idol.userservice.repository.UserViewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSyncConsumer {

    private final UserRepository userRepository;
    private final UserViewRepository userViewRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-update-topic", groupId = "user-sync-group")
    @Transactional
    public void syncUser(String message) {
        try {
            UserEventDto event = objectMapper.readValue(message, UserEventDto.class);
            int userId = event.getUserId();
            String type = event.getType();

            log.info("유저 동기화 이벤트 수신: userId={}, type={}", userId, type);

            if ("DELETE".equals(type)) {
                userViewRepository.deleteById(userId);
                log.info("MongoDB 유저 삭제 완료: userId={}", userId);
                return;
            }

            // MySQL에서 최신 데이터 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found in MySQL: " + userId));

            // MongoDB 저장 (Upsert)
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
                    .createdAt(user.getCreatedAt())
                    .build();

            userViewRepository.save(userView);
            log.info("MongoDB 유저 동기화 완료: userId={}", userId);

        } catch (Exception e) {
            log.error("유저 동기화 처리 중 오류: {}", e.getMessage());
            // 예외를 던지면 Kafka가 재시도(Retry)함
            throw new RuntimeException(e);
        }
    }
}
