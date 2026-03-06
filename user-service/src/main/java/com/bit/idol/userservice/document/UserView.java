package com.bit.idol.userservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "user_views")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserView {
    @Id
    private int id; // MySQL의 PK와 동일하게 사용

    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String address;
    private String imgUrl;

    private String role; // Enum.name() 저장

    private String provider;
    private String providerId;

    private String status; // Enum.name() 저장
    private int reportCount;
    private LocalDateTime suspendedUntil;
    private LocalDateTime createdAt;
}
