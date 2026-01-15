package com.bit.idol.userservice.dto.agency;

import com.bit.idol.userservice.entity.Agency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyDto {

    private Long agencyId;

    @jakarta.validation.constraints.NotBlank(message = "소속사 이름은 필수입니다.")
    private String name;

    public static AgencyDto fromEntity(Agency agency) {
        return AgencyDto.builder()
                .agencyId(agency.getId())
                .name(agency.getName())
                .build();
    }
}
