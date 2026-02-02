package com.bit.idol.userservice.dto.idol;

import com.bit.idol.userservice.entity.Idol;
import com.bit.idol.userservice.entity.IdolStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdolDto {

    private int idolId;
    private int userId;
    private String username;
    private String stageName;
    private String profileImage; // 프로필 이미지 추가
    private int agencyId;
    private String agencyName;
    private IdolStatus status;

    public static IdolDto fromEntity(Idol idol) {
        return IdolDto.builder()
                .idolId(idol.getId())
                .userId(idol.getUser().getId())
                .username(idol.getUser().getUsername())
                .stageName(idol.getStageName())
                .profileImage(idol.getUser().getImgUrl()) // User 엔티티에서 가져옴
                .agencyId(
                        idol.getAgency() != null ? idol.getAgency().getId() : null
                )
                .agencyName(
                        idol.getAgency() != null ? idol.getAgency().getName() : null
                )
                .status(idol.getStatus())
                .build();
    }
}
