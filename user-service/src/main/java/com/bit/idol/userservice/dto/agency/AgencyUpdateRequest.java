package com.bit.idol.userservice.dto.agency;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgencyUpdateRequest {
    @NotBlank(message = "소속사 이름은 필수 입력값입니다.")
    private String name;
}
