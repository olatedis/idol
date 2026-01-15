package com.bit.idol.userservice.dto.idol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IdolRegisterRequest {

    private int userId;

    @jakarta.validation.constraints.NotBlank(message = "활동명은 필수입니다.")
    private String stageName;

    private Long agencyId;
}

