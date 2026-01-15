package com.bit.idol.userservice.dto.agency;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AgencyCreateRequest {

    @jakarta.validation.constraints.NotBlank(message = "소속사 이름은 필수입니다.")
    private String name;
}
