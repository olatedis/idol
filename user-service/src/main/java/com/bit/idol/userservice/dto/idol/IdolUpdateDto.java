package com.bit.idol.userservice.dto.idol;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IdolUpdateDto {
    @NotBlank(message = "스테이지 네임(활동명)은 필수입니다.")
    private String stageName;
}
