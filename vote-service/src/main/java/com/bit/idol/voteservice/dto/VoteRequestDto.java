package com.bit.idol.voteservice.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoteRequestDto {
    @Min(value = 1, message = "후보자 번호는 1 이상이어야 합니다.")
    private int candidateNumber;
}
